package mk.ukim.finki.konsultacii.service.implementation;

import lombok.AllArgsConstructor;
import mk.ukim.finki.konsultacii.model.Semester;
import mk.ukim.finki.konsultacii.repository.SemesterRepository;
import mk.ukim.finki.konsultacii.service.SemesterService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class SemesterServiceImpl implements SemesterService {
    private final SemesterRepository semesterRepository;

    @Override
    public List<Semester> findAll() {
        return semesterRepository.findAll(Sort.by(Sort.Order.desc("year"),
                Sort.Order.asc("semesterType")));
    }

    @Override
    public Semester findByCode(String code) {
        return semesterRepository.findById(code).orElseThrow(() -> new IllegalArgumentException("Semester with code " + code + " not found"));
    }
}
