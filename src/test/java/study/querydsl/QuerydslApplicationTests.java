package study.querydsl;

import static org.assertj.core.api.Assertions.assertThat;

import com.querydsl.jpa.impl.JPAQueryFactory;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.enttiy.Hello;
import study.querydsl.enttiy.QHello;

@Transactional
@SpringBootTest
class QuerydslApplicationTests {

  @Autowired
  EntityManager em;

  @Test
  void contextLoads() {
    Hello hello = new Hello();
    em.persist(hello);

    JPAQueryFactory query = new JPAQueryFactory(em);

    // QHello qHello = new QHello("h");
    QHello qHello = QHello.hello;

    Hello result = query
        .selectFrom(qHello)
        .fetchOne();

    assertThat(result).isEqualTo(hello);
    // lombok 확인인
    assertThat(result.getId()).isEqualTo(hello.getId());
  }

}
