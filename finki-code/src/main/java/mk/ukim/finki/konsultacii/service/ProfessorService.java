package mk.ukim.finki.konsultacii.service;

import mk.ukim.finki.konsultacii.model.Professor;

import java.util.List;
import java.util.TreeMap;


public interface ProfessorService {

    Professor getProfessorById(String id);

    List<Professor> listAllProfessors(String professorName);

    TreeMap<Character, List<Professor>> findAllProfessorsSortedByFirstName(String professorName);
}
