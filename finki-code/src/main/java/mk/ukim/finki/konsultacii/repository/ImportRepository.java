package mk.ukim.finki.konsultacii.repository;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface ImportRepository {

    <T> List<T> readTypeList(MultipartFile file, Class<T> clazz);

    <T> void writeTypeList(Class<T> clazz, List<T> entities, OutputStream outputStream) throws IOException;
}
