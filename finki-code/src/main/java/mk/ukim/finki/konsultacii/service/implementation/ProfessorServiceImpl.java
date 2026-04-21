package mk.ukim.finki.konsultacii.service.implementation;

import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import mk.ukim.finki.konsultacii.model.Professor;
import mk.ukim.finki.konsultacii.model.enumerations.ProfessorTitle;
import mk.ukim.finki.konsultacii.model.exceptions.ProfessorNotFoundException;
import mk.ukim.finki.konsultacii.repository.ProfessorRepository;
import mk.ukim.finki.konsultacii.service.ProfessorService;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.*;

import static mk.ukim.finki.konsultacii.specifications.FieldFilterSpecification.filterContainsText;

@Service
@AllArgsConstructor
public class ProfessorServiceImpl implements ProfessorService {

    private final ProfessorRepository professorRepository;

    @Override
    public Professor getProfessorById(String id) throws ProfessorNotFoundException {
        return professorRepository.findById(id)
                .orElseThrow(() -> new ProfessorNotFoundException("Professor with id " + id + " doesn't exist"));
    }

    @Override
    public List<Professor> listAllProfessors(String professorName) {
        Specification<Professor> spec = Specification.where(null);
        if(professorName!=null && !professorName.isEmpty()){
            Specification<Professor> nameSpec = filterContainsText(Professor.class, "name", professorName);
            Specification<Professor> idSpec = filterContainsText(Professor.class, "id", professorName);

            spec = spec.and(nameSpec.or(idSpec));
        }

        Specification<Professor> excludeSpec = (root, query, criteriaBuilder) -> {
            Predicate notDemonstrator = criteriaBuilder.notLike(root.get("name"), "%Демонстратор%");
            Predicate notIndustry = criteriaBuilder.notLike(root.get("name"), "%Индустрија%");
            Predicate notRetired = criteriaBuilder.notEqual(root.get("title"), ProfessorTitle.RETIRED);
            return criteriaBuilder.and(notDemonstrator, notIndustry, notRetired);
        };

        Specification<Professor> excludeInactiveSpec = (root, query, criteriaBuilder) ->
            criteriaBuilder.isTrue(root.get("active"));

        spec = spec.and(excludeSpec).and(excludeInactiveSpec);

        return professorRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "name"));
    }

    @Override
    public TreeMap<Character, List<Professor>> findAllProfessorsSortedByFirstName(String professorName) {
        List<Character> macedonianAlphabet = Arrays.asList(
                'А', 'Б', 'В', 'Г', 'Д', 'Ѓ', 'Е', 'Ж', 'З', 'Ѕ', 'И', 'Ј',
                'К', 'Л', 'Љ', 'М', 'Н', 'Њ', 'О', 'П', 'Р', 'С', 'Т', 'Ќ',
                'У', 'Ф', 'Х', 'Ц', 'Ч', 'Џ', 'Ш'
        );

        TreeMap<Character, List<Professor>> professorsMap = new TreeMap<>((char1, char2) -> {
            int index1 = macedonianAlphabet.indexOf(char1);
            int index2 = macedonianAlphabet.indexOf(char2);
            return Integer.compare(index1, index2);
        });

        listAllProfessors(professorName).forEach(p ->
        {
            char character = p.getName().toUpperCase().charAt(0);
            if (macedonianAlphabet.contains(character)) {
                List<Professor> professors = professorsMap.getOrDefault(character, new ArrayList<>());
                professors.add(p);
                professorsMap.put(character, professors);
            }
        });
        return professorsMap;
    }
}
