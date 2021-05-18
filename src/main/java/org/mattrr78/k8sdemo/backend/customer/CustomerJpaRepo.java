package org.mattrr78.k8sdemo.backend.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerJpaRepo extends JpaRepository<Customer, Integer> {
}
