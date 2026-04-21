package mk.ukim.finki.konsultacii.service;

import mk.ukim.finki.konsultacii.model.Semester;

import java.util.List;
import java.util.Optional;

public interface SemesterService {

    List<Semester> findAll();

    Semester findByCode(String code);
}
