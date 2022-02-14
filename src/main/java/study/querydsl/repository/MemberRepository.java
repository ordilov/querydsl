package study.querydsl.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.enttiy.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {

  List<Member> findByUsername(String username);

}
