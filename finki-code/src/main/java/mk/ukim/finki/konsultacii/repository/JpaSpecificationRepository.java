package mk.ukim.finki.konsultacii.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;


@NoRepositoryBean
public interface JpaSpecificationRepository<T, ID> extends JpaRepository<T, ID> {
    Page<T> findAll(Specification<T> filter, Pageable page);

    List<T> findAll(Specification<T> filter);

    List<T> findAll(Specification<T> filter, Sort sort);
}
