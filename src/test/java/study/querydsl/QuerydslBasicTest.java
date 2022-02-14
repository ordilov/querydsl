package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.enttiy.QMember.member;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Objects;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
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
  public void resultFetch(){
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

}