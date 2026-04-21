package mk.ukim.finki.konsultacii.repository;

import mk.ukim.finki.konsultacii.model.User;
import mk.ukim.finki.konsultacii.model.enumerations.UserRole;

import java.util.List;


public interface UserRepository extends JpaSpecificationRepository<User, String> {

}
