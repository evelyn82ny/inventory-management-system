# 동시성 이슈 해결

멀티스레드 환경에서 발생하는 **동시성 이슈를 해결**하고 **빠른 응답 속도를 목표**로 한다.
<br>

멀티스레드 환경으로 테스트하는 이유는 WAS(Web Applicaton Server)로 사용 중인 Tomcat은 요청마다 Thread가 할당되는 멀티스레드 환경이기 때문이다. Pessimistic Lock을 사용해 동시성 이슈를 쉽게 해결할 수 있지만, 모든 트랜잭션이 직렬화되어 그만큼 응답 속도가 느려지므로 *빠른 속도로 개선할 수 있는 방법을 찾는 것이 이 프로젝트의 목표*이다.

## 실행 환경

- Docker Compose-based MySQL
- JDK 11

## Issue

### ❗️[Concurrency Problem: Lost Update](#concurrency-problem-lost-update-발생)
 
- [Solution1: Java synchronized](#solution1-java-synchronized)
- [Solution2: Pessimistic Lock](#solution2-pessimistic-lock)

### ❗️[Concurrency Problem: Deadlock](#concurrency-problem-deadlock-발생)

<br>

## Transaction Isolation Level

데이터베이스는 트랜잭션 격리를 제공해 동시성 문제를 감춰주지만, 모든 이유로부터는 보호하기 힘들기 때문에 완화된 격리 수준을 사용하는 시스템이 흔하다. 
완화된 격리 수준에는 Read Committed, Snapshot Isolation, Repeatable Read 등이 있다.

- Read Committed: Oracle의 기본 설정
- Repeatable Read: MySQL InnoDB 엔진의 기본 설정

### MVCC (Multi-Version Concurrency Control)

- MVCC(Multi-Version Concurrency Control)는 **잠금을 사용하지 않고**도 일관된 읽기는 제공하는 것이 목적이다.
- MySQL의 InnoDB 스토리지 엔진에서는 Undo Log를 이용해 MVCC 기능을 구현한다.
  <br>

- READ-COMMITTED 격리 수준에서는 과거에 커밋된 값과 현재 쓰기 잠금을 갖고 있는 트랜잭션이 갱신한 값(아직 커밋하지 않는 데이터)을 기억하므로 Dirty Read가 발생하지 않는다.
- 하지만 Non-repeatable Read가 발생한다. (한 트랜잭션에서 같은 SELECT 쿼리를 여러번 발생했을 때 결과가 다른 현상)
- 이는 객체에 대해 2가지 값(커밋된 값과, 갱신하려고 하는 값)만 가지고 있어 발생하는 문제이며, 이에 대한 해결책이 Snapshot Isolation이다.
  <br>

- Snapshot Isolation(스냅숏 격리)는 객체에 대해 여러가지 버전의 값을 가지고 있으며, 트랜잭션은 같은 버전의 데이터만 읽어 Non-repeatabe read가 발생하지 않는 일관된 읽기를 할 수 있다.
- 이 스냅숏 격리를 MySQL에서는 Repeatable read라고 한다.

<br>

## ❗️ Concurrency problem: Lost Update 발생

- MySQL InnoDB 엔진을 사용했기 때문에 Dirty Read, Non-repeatable read 그리고 Phantom read 현상이 발생하지 않는다.
    - MySQL InnoDB 엔진은 Next Key Lock을 사용해 범위를 잠궈 Phantom Read 현상을 막는다.
- 커밋된 데이터만 접근할 수 있고 특정 버전만 읽을 수 있음에도, Lost Update(갱신 손실)기 발생해 재고보다 많은 주문이 생성되는 동시성 문제가 발생했다.
  <br>

- 초기 로직은 잠금 없이 Item 객체를 가져와 재고가 있다면 주문을 생성한다.
- 이때 Item 객체에 어떠한 잠금도 획득하지 않기 때문에 여러 트랜잭션이 같은 객체에 접근할 수 있다.
- 이는 여러 트랜잭션이 같은 객체에 대해 Shared-Lock 획득이 가능한 것과 같은 상황이다.
  <br>

- 모든 트랜잭션이 같은 객체에 대해 어떠한 잠금 없이 객체에 접근했고, MySQL InnoDB 엔진을 사용하므로 모든 트랜잭션은 같은 Item 객체에 대해 같은 버전을 읽는다.
- 그러므로 같은 상태를 기반을 자신만의 주문을 만들지만, **재고 감소도 같은 상태를 기반으로 갱신**되므로 생성된 주문의 개수보다 더 적게 감소하는 **갱신 손실이 발생**한다.
    - HikariCP Connection이 10개를 제공하도록 설정했고, 30개의 스레드가 요청을 처리한다고 가정하면 30개 중 커넥션을 받은 첫 10개의 스레드만 같은 버전일 가능성이 높고, 이후에는 제각각의 버전을 볼 가능성이 있다. 하지만 결론은 재고보다 많은 주문이 생성된다는 것이다.
- 발생한 동시성을 해결하는 방법은 synchronized, pessimistic lock, optimistic lock, redisson 등이 존재한다.

<br>

## Solution1: Java synchronized

Java는 synchronized 키워드를 제공해 thread-safe를 가능하게 한다.

```java
public synchronized void decreaseQuantity(Long id, Long quantity) {
    Item item = itemRepository.findById(id).orElseThrow();
    item.decreaseQuantity(quantity);
    itemRepository.saveAndFlush(item);
}
```
> commit: https://github.com/evelyn82ny/inventory-management-system/commit/79f601ad9abdf9486b3d5cafbd8305d30e3c9353

<br>

위와 같이 ```synchronized``` 키워드를 사용하면 Lost update가 발생하지 않아 재고만큼만 주문을 생성할 수 있다.
즉, 멀티스레드 환경에서 발생하는 동시성 문제를 해결할 수 있다.
<br>

하지만 ```synchronized``` 키워드는 인스턴스 단위로만 thread-safe가 보장된다.
서버 인스턴스가 1개인 경우에만 효과 있고, 여러 서버를 사용하게 된다면 여러 인스턴스가 존재하는 것이므로  ```synchronized``` 키워드를 사용해도 동시성 문제를 해결할 수 없다.

<br>

## Solution2: Pessimistic Lock

Shared-Lock 또는 Exclustive-Lock 을 사용하며 다음과 같은 특징이 있다.

- 같은 레코드에 대해 여러 트랜잭션은 Shared-Lock을 획득할 수 있다.
- 같은 레코드에 대해 1개의 트랜잭션만 Exclusive-Lock을 획득할 수 있다.
- Exclustive-Lock을 획득하기 위해선 다른 세션의 Shared-Lock과 Exclusive-Lock을 획득이 얻어야 한다.
- 이미 Shared-Lock이 걸려있는 상태라면 Exclusive-Lock을 획득할 수 없다.
- 이미 Exclusive-Lock이 걸려있는 상태라면 Shared-Lock 또는 Exclusive-Lock을 획득할 수 없다.

```java
public interface ItemRepository extends JpaRepository<Item, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Item i where i.id=:id")
    Item findByIdWithPessimisticLock(@Param("id") Long id);
}
```
```java
@Transactional
public void decreaseQuantity(Long id, Long quantity) {
    Item item = itemRepository.findByIdWithPessimisticLock(id);
    item.decreaseQuantity(quantity);
    itemRepository.saveAndFlush(item);
}
```

> commit: https://github.com/evelyn82ny/inventory-management-system/commit/3c457e4c4985dcf98df2a9833e43f9ce33ca4772

<br>

- 위와 같이 작성하면 item 레코드를 가지고 오는 쿼리에 FOR UPDATE 추가
- X-Lock을 획득한 트랜잭션이 종료(commit or abort)해야 다른 트랜잭션이 락을 획득할 수 있으므로 동시성 문제 해결
<br>

하지만 또 다른 문제가 발생한다. 이미 X-Lock을 걸려있는 상태에서 다른 세션도 수정 작업을 한다면 X-Lock을 획득하기 위해 기다리므로 위와 같은 방식으로 많은 요청을 처리한다면 응답 속도가 상당히 늦어질 것이다.

- 현재 MSA(MicroService Architecture)를 공부하는 중인데 주문 서비스, 결제 서비스, 고객 서비스로 분리해 분산 트랜잭션 처리를 지원하는 Saga 패턴과 이벤트 기반 비동기 통신을 적용하면 응답 속도 문제를 해결할 것이라 예상한다.

<br>

## ❗️ Concurrency problem: Deadlock 발생

멀티스레드 환경에서 발생하는 동시성 문제 중 하나인 Deadlock이 발생했다.

![png](/_img/diagram.png)

ORDER는 ACCOUNT와 ITEM을 FK로 참조하는 구조이다.

> If a FOREIGN KEY constraint is defined on a table, any insert, update, or delete 
> that requires the constraint condition to be checked sets shared record-level locks on the records that it looks at to check the constraint. 
> InnoDB also sets these locks in the case where the constraint fails.
> https://dev.mysql.com/doc/refman/5.6/en/innodb-locks-set.html

FK를 가지고 있는 테이블이 INSERT, UPDATE 또는 DELETE 작업을 한다면 제약 조건을 확인하기 위해 FK로 참조하는 레코드에 S-Lock이 걸린다.
<br>

```java
@Transactional
public Boolean create(Long accountId, RequestOrder request) {
    Account account = accountRepository.findById(accountId).orElseThrow();
    Item item = itemRepository.findById(request.itemId).orElseThrow();

    // 재고 감소
    if (!item.decreaseQuantity(request.count)) return false;

    // 재고 감소 성공하면 주문 생성
    Order order = Order.builder()
                .account(account)
                .item(item)
                .count(request.count)
                .build();

    // ORDER를 INSERT하면 FK로 참조하는 ACCOUNT와 ITEM 레코드에 S-LOCK이 걸린다.
    orderRepository.save(order); 

    account.increaseNumberOfOrders();
    return true;
}
```
- ORDER 객체가 생성되었다는 것은 재고 감소에 성공했다는 의미
- ORDER 객체가 INSERT되면 ITEM 객체도 재고 감소를 반영하기 위해 UPDATE 시도
- ORDER 객체가 INSERT할 때 MySQL에서 제약 조건을 확인하기 위해 ITEM 객체에 S-Lock을 획득을 시도
- Shared-Lock끼리는 호환 가능므로 멀티스레드 환경에서 여러 트랜잭션이 동시에 ORDER 객체를 생성할 수 있음
<br>

- 재고 감소로 인해 ITEM 객체를 UPDATE하기 위해 X-Lock을 획득 시도
- 멀티스레드 환경에서 테스트하면 모든 스레드가 같은 ITEM 레코드에 대해 S-Lock을 획득한 상태
- X-Lock을 획득하기 위해선 어떠한 락도 걸려있으면 안되는데 다른 세션에서 S-Lock을 획득한 상태이므로 S-Lock을 획득한 세션이 종료해야만 X-Lock을 획득할 수 있음
- 하지만 모든 트랜잭션은 다른 트랜잭션이 S-Lock을 반납하길 바라는데 자신도 같은 상태이므로 결국 X-Lock을 획득하기 못하는 Deadlock 발생
<br>

> 데드락이 발생한 테스트 코드의 커밋: https://github.com/evelyn82ny/inventory-management-system/commit/db7cf746e9e420b07b8fb2580291a29c8c0cca46

```text
LATEST DETECTED DEADLOCK

*** (1) TRANSACTION:
TRANSACTION 20452, ACTIVE 0 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 9 lock struct(s), heap size 1128, 4 row lock(s), undo log entries 2
MySQL thread id 130, OS thread handle 281472028057536, query id 5089 172.17.0.1 root updating
update item set quantity=3 where id=1

*** (1) HOLDS THE LOCK(S):
RECORD LOCKS space id 303 page no 4 n bits 72 index PRIMARY of table inventory_management.item trx id 20452 
lock mode S locks rec but not gap
Record lock, heap no 2 PHYSICAL RECORD: n_fields 4; compact format; info bits 0
0: len 8; hex 8000000000000001; asc ;;
1: len 6; hex 000000004fcc; asc O ;;
2: len 7; hex 01000001ca1aa5; asc ;;
3: len 8; hex 8000000000000004; asc ;;

*** (1) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 303 page no 4 n bits 72 index PRIMARY of table inventory_management.item trx id 20452 
lock_mode X locks rec but not gap waiting
Record lock, heap no 2 PHYSICAL RECORD: n_fields 4; compact format; info bits 0
0: len 8; hex 8000000000000001; asc ;;
1: len 6; hex 000000004fcc; asc O ;;
2: len 7; hex 01000001ca1aa5; asc ;;
3: len 8; hex 8000000000000004; asc ;;

*** (2) TRANSACTION:
TRANSACTION 20455, ACTIVE 0 sec starting index read
mysql tables in use 1, locked 1
LOCK WAIT 9 lock struct(s), heap size 1128, 4 row lock(s), undo log entries 2
MySQL thread id 137, OS thread handle 281471507963840, query id 5093 172.17.0.1 root updating
update item set quantity=3 where id=1

*** (2) HOLDS THE LOCK(S):
RECORD LOCKS space id 303 page no 4 n bits 72 index PRIMARY of table inventory_management.item trx id 20455 
lock mode S locks rec but not gap
Record lock, heap no 2 PHYSICAL RECORD: n_fields 4; compact format; info bits 0
0: len 8; hex 8000000000000001; asc ;;
1: len 6; hex 000000004fcc; asc O ;;
2: len 7; hex 01000001ca1aa5; asc ;;
3: len 8; hex 8000000000000004; asc ;;

*** (2) WAITING FOR THIS LOCK TO BE GRANTED:
RECORD LOCKS space id 303 page no 4 n bits 72 index PRIMARY of table inventory_management.item trx id 20455 
lock_mode X locks rec but not gap waiting
Record lock, heap no 2 PHYSICAL RECORD: n_fields 4; compact format; info bits 0
0: len 8; hex 8000000000000001; asc ;;
1: len 6; hex 000000004fcc; asc O ;;
2: len 7; hex 01000001ca1aa5; asc ;;
3: len 8; hex 8000000000000004; asc ;;

*** WE ROLL BACK TRANSACTION (2)
```

<br>

## Reference

- Docker(MySQL): https://velog.io/@_nine/Docker-MySQL%EC%84%A4%EC%B9%98-%EB%B0%8F-%EC%A0%91%EC%86%8D%ED%95%98%EA%B8%B0