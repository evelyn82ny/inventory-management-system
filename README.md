# 동시성 이슈 해결

멀티스레드 환경에서 발생하는 **동시성 이슈를 해결**하고 **빠른 응답 속도를 목표**로 한다.
<br>

멀티스레드 환경으로 테스트하는 이유는 WAS(Web Applicaton Server)로 사용 중인 Tomcat은 요청마다 Thread가 할당되는 멀티스레드 환경이기 때문이다. Pessimistic Lock을 사용해 동시성 이슈를 쉽게 해결할 수 있지만, 모든 트랜잭션이 직렬화되어 그만큼 응답 속도가 느려지므로 *빠른 속도로 개선할 수 있는 방법을 찾는 것이 이 프로젝트의 목표*이다.

## 실행 환경

- Docker Compose-based MySQL
- JDK 11

<br>

## Transaction Isolation Level

데이터베이스는 트랜잭션 격리를 제공해 동시성 문제를 감춰주지만, 모든 이유로부터는 보호하기 힘들기 때문에 완화된 격리 수준을 사용하는 시스템이 흔하다. 완화된 격리 수준에는 Read Committed, Snapshot Isolation, Repeatable Read 등이 있다.

- Read Committed: Oracle의 기본 설정
- Repeatable Read: MySQL InnoDB 엔진의 기본 설정

### MVCC (Multi-Version Concurrency Control)

레코드 레벨의 트랜잭션을 지원하는 DBMS가 제공하는 기능으로, **잠금을 사용하지 않고도 일관된 읽기는 제공**하는 것이 목적이다. MySQL의 InnoDB 스토리지 엔진에서는 Undo Log를 이용해 MVCC 기능을 구현한다.
<br>

잠금을 사용하지 않고도 일관된 읽기가 제공되므로 READ-COMMITTED 격리 수준에서 발생하는 Non-Repeatable Read 부정합이 발생하지 않는다. READ-COMMITTED 격리 수준에서는 과거에 커밋된 값과 현재 쓰기 잠금을 갖고 있는 트랜잭션이 갱신한 값(아직 커밋하지 않는 데이터)을 기억한다. 즉, 객체에 대해 2가지 값을 가지고 있어 하나의 트랜잭션 내에서 똑같은 SELECT 쿼리를 실행했을 때 항상 같은 결과를 가져오지 못할 수 있다.
<br>

이에 대한 해결책이 Snapshot Isolation이다. 스냅숏 격리는 객체에 대해 여러가지 버전의 값을 가지고 있어 트랜잭션은 같은 버전의 데이터만 읽어 Non-repeatabe read가 발생하지 않는 일관된 읽기를 할 수 있다. 이 스냅숏 격리를 MySQL에서는 Repeatable read라고 한다.

<br>

## Lost Update 발생

MySQL InnoDB 엔진을 사용했기 때문에 Dirty Read, Non-repeatable read 그리고 Phantom read 현상이 발생하지 않는다. 커밋된 데이터만 접근할 수 있고 특정 버전만 읽을 수 있음에도 Lost Update(갱신 손실)기 발생해 재고보다 많은 주문이 생성되었다. 이것이 바로 멀티스레드 환경에서 발생하는 동시성 문제이다.
<br>

초기 로직은 잠금 없이 Item 객체를 가져와 재고가 있다면 주문을 생성한다. 이때 Item 객체에 어떠한 잠금도 획득하지 않기 때문에 여러 트랜잭션이 같은 객체에 접근할 수 있다. 여러 트랜잭션이 같은 객체에 대해 Shared-Lock 획득이 가능한 것과 같은 상황이다.

모든 트랜잭션이 같은 객체에 대해 어떠한 잠금 없이 객체에 접근했고, MySQL InnoDB 엔진을 사용하므로 모든 트랜잭션은 같은 Item 객체에 대해 같은 버전을 읽는다. 그러므로 같은 상태를 기반을 자신만의 주문을 만들지만, 재고 감소도 같은 상태를 기반으로 커밋되므로 생성된 주문의 개수보다 더 적게 감소하는 갱신 손실이 발생한다.
<br>

더 자세히 설명하자면 HikariCP Connection이 10개가 제공되고, 30개의 스레드가 요청을 처리한다고 가정하면 30개 중 커넥션을 받은 첫 10개의 스레드만 같은 버전일 가능성이 높고, 이후에는 제각각의 버전을 볼 가능성이 있다. 하지만 결론은 재고보다 많은 주문이 생성된다는 것이다. 발생한 동시성을 해결하는 방법은 synchronized, pessimistic lock, optimistic lock, redisson 등이 존재한다.

<br>

## Reference

- Docker(MySQL):https://velog.io/@_nine/Docker-MySQL%EC%84%A4%EC%B9%98-%EB%B0%8F-%EC%A0%91%EC%86%8D%ED%95%98%EA%B8%B0