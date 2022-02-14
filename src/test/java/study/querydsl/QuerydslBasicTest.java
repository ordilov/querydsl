package study.querydsl;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.enttiy.QMember.member;
import static study.querydsl.enttiy.QTeam.team;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Objects;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.enttiy.Member;
import study.querydsl.enttiy.QMember;
import study.querydsl.enttiy.Team;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

  @Autowired
  EntityManager em;

  JPAQueryFactory queryFactory;

  @BeforeEach
  public void before() {
    queryFactory = new JPAQueryFactory(em);
    Team teamA = new Team("teamA");
    Team teamB = new Team("teamB");
    em.persist(teamA);
    em.persist(teamB);

    Member member1 = new Member("member1", 10, teamA);
    Member member2 = new Member("member2", 20, teamA);

    Member member3 = new Member("member3", 30, teamB);
    Member member4 = new Member("member4", 40, teamB);

    em.persist(member1);
    em.persist(member2);
    em.persist(member3);
    em.persist(member4);
  }

  @Test
  public void startJPQL() {
    //member1을 찾아라.
    String qlString =
        "select m from Member m "
            + "where m.username = :username";
    Member findMEmber = em.createQuery(qlString,
            Member.class)
        .setParameter("username", "member1")
        .getSingleResult();

    assertThat(findMEmber.getUsername()).isEqualTo("member1");
  }

  @Test
  public void startQuerydsl() {
    Member findMember = queryFactory
        .select(member)
        .from(member)
        .where(member.username.eq("member1"))
        .fetchOne();

    assertThat(Objects.requireNonNull(findMember).getUsername()).isEqualTo("member1");
  }

  @Test
  public void search() {
    Member findMember = queryFactory
        .selectFrom(member)
        .where(member.username.eq("member1")
            .and(member.age.eq(10)))
        .fetchOne();

    assertThat(Objects.requireNonNull(findMember).getUsername()).isEqualTo("member1");
  }

  @Test
  public void searchAndParam() {
    Member findMember = queryFactory
        .selectFrom(member)
        .where(
            member.username.eq("member1"),
            member.age.eq(10)
        )
        .fetchOne();

    assertThat(Objects.requireNonNull(findMember).getUsername()).isEqualTo("member1");
  }

  @Test
  public void resultFetch() {
//    List<Member> fetch = queryFactory
//        .selectFrom(member)
//        .fetch();
//
//    Member fetchOne = queryFactory
//        .selectFrom(QMember.member)
//        .fetchOne();
//
//    Member firstFetch = queryFactory
//        .selectFrom(member)
//        .fetchFirst();

    QueryResults<Member> results = queryFactory
        .selectFrom(member)
        .fetchResults();

    results.getTotal();
    List<Member> content = results.getResults();

    long totalCount = queryFactory
        .selectFrom(member)
        .fetchCount();
  }

  @Test
  public void count() {
    Long totalCount = queryFactory
        //.select(Wildcard.count) //select count(*)
        .select(member.count()) //select count(member.id)
        .from(member)
        .fetchOne();
    System.out.println("totalCount = " + totalCount);
  }

  /**
   * 회원 정렬 순서
   * 1. 회원 나이 내림차순(desc)
   * 2. 회원 이름 올림차순(asc)
   * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
   */
  @Test
  public void sort() {
    em.persist(new Member(null, 100));
    em.persist(new Member("member5", 100));
    em.persist(new Member("member6", 100));

    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.eq(100))
        .orderBy(member.age.desc(), member.username.asc().nullsLast())
        .fetch();

    Member member5 = result.get(0);
    Member member6 = result.get(1);
    Member memberNull = result.get(2);
    assertThat(member5.getUsername()).isEqualTo("member5");
    assertThat(member6.getUsername()).isEqualTo("member6");
    assertThat(memberNull.getUsername()).isNull();
  }

  @Test
  public void paging1() {
    List<Member> result = queryFactory
        .selectFrom(member)
        .orderBy(member.age.desc())
        .offset(1)
        .limit(2)
        .fetch();

    assertThat(result.size()).isEqualTo(2);
  }

  @Test
  public void paging2() {
    QueryResults<Member> queryResults = queryFactory
        .selectFrom(member)
        .orderBy(member.age.desc())
        .offset(1)
        .limit(2)
        .fetchResults();

    assertThat(queryResults.getTotal()).isEqualTo(4);
    assertThat(queryResults.getLimit()).isEqualTo(2);
    assertThat(queryResults.getOffset()).isEqualTo(1);
    assertThat(queryResults.getResults().size()).isEqualTo(2);
  }

  @Test
  public void aggregation() {
    List<Tuple> result = queryFactory
        .select(
            member.count(),
            member.age.sum(),
            member.age.avg(),
            member.age.max(),
            member.age.min()
        )
        .from(member)
        .fetch();

    Tuple tuple = result.get(0);
    assertThat(tuple.get(member.count())).isEqualTo(4);
    assertThat(tuple.get(member.age.sum())).isEqualTo(100);
    assertThat(tuple.get(member.age.avg())).isEqualTo(25);
    assertThat(tuple.get(member.age.max())).isEqualTo(40);
    assertThat(tuple.get(member.age.min())).isEqualTo(10);
  }

  /**
   * 팀의 이름과 각 팀의 평균 연령을 구해라.
   */
  @Test
  public void group() throws Exception {
    List<Tuple> result = queryFactory
        .select(
            team.name,
            member.age.avg()
        )
        .from(member)
        .join(member.team, team)
        .groupBy(team.name)
        .fetch();

    Tuple teamA = result.get(0);
    Tuple teamB = result.get(1);

    assertThat(teamA.get(team.name)).isEqualTo("teamA");
    assertThat(teamA.get(member.age.avg())).isEqualTo(15);
    assertThat(teamB.get(team.name)).isEqualTo("teamB");
    assertThat(teamB.get(member.age.avg())).isEqualTo(35);
  }

  /**
   * 팀 A에 소속된 모든 회원
   */
  @Test
  public void join() {
    List<Member> result = queryFactory
        .selectFrom(member)
        .join(member.team, team)
        .where(team.name.eq("teamA"))
        .fetch();

    assertThat(result)
        .extracting("username")
        .containsExactly("member1", "member2");
  }

  @Test
  public void theta_join() {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));
    em.persist(new Member("teamC"));

    List<Member> result = queryFactory
        .select(member)
        .from(member, team)
        .where(member.username.eq(team.name))
        .fetch();

    assertThat(result)
        .extracting("username")
        .containsExactly("teamA", "teamB");
  }

  /**
   * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
   * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
   */
  @Test
  public void join_on_filtering() {
    List<Tuple> result = queryFactory
        .select(member, team)
        .from(member)
        .leftJoin(member.team, team)
        .on(team.name.eq("teamA"))
        .fetch();

    result.forEach(System.out::println);
  }

  /**
   * 연관관계 없는 엔티티 외부 조인
   * 회원의 이름이 팀 이름과 같은 대상 외부 조인
   */
  @Test
  public void join_on_no_relation() {
    em.persist(new Member("teamA"));
    em.persist(new Member("teamB"));
    em.persist(new Member("teamC"));

    List<Tuple> result = queryFactory
        .select(member, team)
        .from(member)
        .leftJoin(team)
        .on(member.username.eq(team.name))
        .fetch();

    result.forEach(System.out::println);
  }

  @PersistenceUnit
  EntityManagerFactory emf;

  @Test
  public void fetchJoinNo() {
    em.flush();
    em.clear();

    Member findMember = queryFactory
        .selectFrom(member)
        .where(member.username.eq("member1"))
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).as("페치 조인 미적용").isFalse();
  }

  @Test
  public void fetchJoinUse() {
    em.flush();
    em.clear();

    Member findMember = queryFactory
        .selectFrom(member)
        .join(member.team, team).fetchJoin()
        .where(member.username.eq("member1"))
        .fetchOne();

    boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
    assertThat(loaded).as("페치 조인 적용").isTrue();
  }

  /**
   * 나이가 가장 많은 회원 조회
   */
  @Test
  public void subQuery(){
    QMember memberSub = new QMember("memberSub");
    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.eq(
            select(memberSub.age.max())
                .from(memberSub)
        ))
        .fetch();

    assertThat(result).extracting("age")
        .containsExactly(40);
  }

  /**
   * 나이가 평균 이상인 회원
   */
  @Test
  public void subQueryGoe(){
    QMember memberSub = new QMember("memberSub");
    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.goe(
            select(memberSub.age.avg())
                .from(memberSub)
        ))
        .fetch();

    assertThat(result).extracting("age")
        .containsExactly(30, 40);
  }

  /**
   *  서브쿼리 여러 건 처리, in 사용
   */
  @Test
  public void subQueryIn(){
    QMember memberSub = new QMember("memberSub");
    List<Member> result = queryFactory
        .selectFrom(member)
        .where(member.age.in(
            select(memberSub.age)
                .from(memberSub)
                .where(memberSub.age.gt(10))
        ))
        .fetch();

    assertThat(result).extracting("age")
        .containsExactly(20, 30, 40);
  }

  @Test
  public void selectSubQuery() {
    QMember memberSub = new QMember("memberSub");
    List<Tuple> result = queryFactory
        .select(member.username,
            select(memberSub.age.avg())
                .from(memberSub))
        .from(member)
        .fetch();

    result.forEach(System.out::println);
  }

  @Test
  public void basicCase() {
    List<String> result = queryFactory
        .select(member.age
            .when(10).then("열살")
            .when(20).then("스무살")
            .otherwise("기타"))
        .from(member)
        .fetch();

    result.forEach(System.out::println);
  }

  @Test
  public void complexCase(){
    List<String> result = queryFactory
        .select(new CaseBuilder()
            .when(member.age.between(0, 20)).then("0~20살")
            .when(member.age.between(21, 30)).then("21~30살")
            .otherwise("기타"))
        .from(member)
        .fetch();

    result.forEach(System.out::println);
  }

  @Test
  public void constant(){
    List<Tuple> result = queryFactory
        .select(member.username, Expressions.constant("A"))
        .from(member)
        .fetch();

    result.forEach(System.out::println);
  }


  @Test
  public void concat(){
    //{username}_{age}
    List<String> result = queryFactory
        .select(member.username.concat("_").concat(member.age.stringValue()))
        .from(member)
        .where(member.username.eq("member1"))
        .fetch();

    result.forEach(System.out::println);
  }

  @Test
  public void simpleProjection(){
    List<String> result = queryFactory
        .select(member.username)
        .from(member)
        .fetch();

    result.forEach(System.out::println);
  }

  @Test
  public void tupleProjection(){
    List<Tuple> result = queryFactory
        .select(member.username, member.age)
        .from(member)
        .fetch();

    result.forEach(t -> {
      String username = t.get(member.username);
      Integer age = t.get(member.age);
      System.out.println("username = " + username);
      System.out.println("age = " + age);
    });
  }

  @Test
  public void findDtoByJPQL(){
    List<MemberDto> result = em.createQuery(
            "select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
        .getResultList();
    result.forEach(System.out::println);
  }

  @Test
  public void findDtoBySetter(){
    List<MemberDto> result = queryFactory
        .select(Projections.bean(MemberDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();

    result.forEach(System.out::println);
  }

  @Test
  public void findDtoByField(){
    List<MemberDto> result = queryFactory
        .select(Projections.fields(MemberDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();

    result.forEach(System.out::println);
  }

  @Test
  public void findDtoByConstructor(){
    List<MemberDto> result = queryFactory
        .select(Projections.constructor(MemberDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();

    result.forEach(System.out::println);
  }

  @Test
  public void findUserDto() {
    List<UserDto> result = queryFactory
        .select(Projections.fields(UserDto.class,
            member.username.as("name"),
            member.age))
        .from(member)
        .fetch();

    result.forEach(System.out::println);
  }

  @Test
  public void findUserDtoSubQuery() {
    QMember memberSub = new QMember("memberSub");
    List<UserDto> result = queryFactory
        .select(Projections.fields(UserDto.class,
            member.username.as("name"),
            ExpressionUtils.as(JPAExpressions
                .select(memberSub.age.max())
                .from(memberSub), "age")
            ))
        .from(member)
        .fetch();

    result.forEach(System.out::println);
  }

  @Test
  public void findUserDtoByConstructor() {
    List<UserDto> result = queryFactory
        .select(Projections.constructor(UserDto.class,
            member.username,
            member.age))
        .from(member)
        .fetch();

    result.forEach(System.out::println);
  }
}
