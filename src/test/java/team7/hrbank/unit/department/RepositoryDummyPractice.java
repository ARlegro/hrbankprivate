package team7.hrbank.unit.department;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import net.datafaker.Faker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StringUtils;
import team7.hrbank.config.QuerydslConfig;
import team7.hrbank.domain.department.dto.DepartmentCreateRequest;
import team7.hrbank.domain.department.dto.DepartmentMapperImpl;
import team7.hrbank.domain.department.entity.Department;
import team7.hrbank.domain.department.repository.DepartmentRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.*;
//@DataJpaTest를 사용하는 경우 기본적으로 Auditing 기능이 활성화되지 않으므로,
// 테스트 설정에 별도로 Auditing 설정을 포함하거나 필요한 설정 클래스를 @Import 어노테이션으로 불러오도록 구성

@Import({QuerydslConfig.class, DepartmentMapperImpl.class})
@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
public class RepositoryDummyPractice {

    private static final Logger log = LoggerFactory.getLogger(RepositoryDummyPractice.class);

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private DepartmentMapperImpl departmentMapper;

    @Test
        //@BeforeEach
    void setUp() throws IOException {
        ClassPathResource jsonData = new ClassPathResource("department100.json");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // LocalDateTime, LocalDate 등 자바 8 모듈 추가

        List<DepartmentCreateRequest> dtoList = objectMapper.readValue(jsonData.getInputStream(), new TypeReference<List<DepartmentCreateRequest>>() {
        });
        List<String> nameList = dtoList.stream().map(DepartmentCreateRequest::name).toList();

        assertThat(dtoList).hasSize(100);
        assertThat(nameList).doesNotHaveDuplicates();

        dtoList.forEach(dto -> departmentRepository.save(departmentMapper.toEntity(dto)));
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("ObjectMapper로 JSON 파싱 테스트 + 모듈 추가")
    void parseFilterJsonTest() throws IOException {
        ClassPathResource jsonData = new ClassPathResource("department100.json");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // LocalDateTime, LocalDate 등 자바 8 모듈 추가

        List<DepartmentCreateRequest> jsonDtoList = objectMapper.readValue(jsonData.getInputStream(), new TypeReference<List<DepartmentCreateRequest>>() {
        });
        List<String> nameList = jsonDtoList.stream().map(DepartmentCreateRequest::name).toList();

        assertThat(jsonDtoList).hasSize(100);
        assertThat(nameList).doesNotHaveDuplicates(); // 이름 중복 검증
    }

    @Test
    @DisplayName("Fake 라이터리로 더미 데이터 만들어보기")
    void makeFakeDummy() {
        int settingSize = 30;
        // 공통되는 단어 신경 안쓸때
        String commonWord = "ㅎㅇ";

        setting_entity_save_and_containing_name(settingSize, commonWord);
        setting_entity_save_and_containing_description(settingSize, commonWord);

        List<Department> all = departmentRepository.findAll();

        assertThat(all).hasSize(settingSize);
        assertThat(all).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("부서 생성 JSON 테스트")
    void createTest() {
        // given


        // when

        em.flush();
        em.clear();

        // then

    }


    @Test
    @DisplayName("제대로 정렬이 되는지 테스트")
    void findTest() {


    }

    //  @Override
    //  public PageDepartmentsResponseDto findDepartments(String nameOrDescription,
    //                                                    Integer idAfter,
    //                                                    String cursor,
    //                                                    Integer size,
    //                                                    String sortField,
    //                                                    String sortDirection)

    private void setting_entity_save_and_containing_name(int size, String containingword) {
        Faker faker = new Faker();
        Set<String> departmentName = new HashSet<>();



        while (departmentName.size() <= size) {
            String name = faker.company().name();
            int randomNum = StringUtils.hasText(containingword)
                    ? (int) (Math.random() * 3) + 1
                    : -1;

            switch (randomNum) {
                case 1 -> name = name + " " + faker.company().suffix();
                case 2 -> name = faker.company().name() + " " + faker.company().profession();
                case 3 -> {
                    String zeroIndex = name.substring(0, 1);
                    String oneIndex = name.substring(1);
                    name = zeroIndex + containingword + oneIndex;
                }
            }

            departmentName.add(name);
        }

        List<String> departmentDescription = new ArrayList<>();
        List<LocalDate> establishedDate = new ArrayList<>();
        while (departmentDescription.size() <= size) {
            String description = faker.lorem().paragraph(1);
            departmentDescription.add(description);

            LocalDate randomDate = faker.date().birthdayLocalDate();
            establishedDate.add(randomDate);
        }

        List<String> nameList = departmentName.stream().toList();

        List<Department> departmentList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String name = nameList.get(i);
            String description = departmentDescription.get(i);
            LocalDate date = establishedDate.get(i);
            departmentList.add(new Department(name, description, date));
        }
        departmentRepository.saveAllAndFlush(departmentList);
        em.flush();
        em.clear();
    }


    private void setting_entity_save_and_containing_description(int countNumber, String containingword) {
        Faker faker = new Faker();
        Set<String> departmentName = new HashSet<>();

        while (departmentName.size() <= countNumber) {
            String name = faker.company().name();
            departmentName.add(name);
        }


        List<String> departmentDescription = new ArrayList<>();
        while (departmentDescription.size() <= countNumber) {
            String description = faker.lorem().paragraph(1);
            int randomNum = StringUtils.hasText(containingword)
                    ? (int) (Math.random() * 3) + 1
                    : -1;
            switch (randomNum) {
                case 1 -> description = description + " " + faker.company().suffix();
                case 2 -> description = faker.company().name() + " " + faker.company().profession();
                case 3 -> {
                    String zeroIndex = description.substring(0, 1);
                    String oneToEndIndex = description.substring(1);

                    description = zeroIndex + containingword + oneToEndIndex;
                }
            }
            departmentDescription.add(description);
        }

        List<LocalDate> establishedDate = new ArrayList<>();
        while (establishedDate.size() <= countNumber) {
            LocalDate randomDate = faker.date().birthdayLocalDate();
            establishedDate.add(randomDate);
            //LocalDate randomDate2 = faker.date().past(3650, TimeUnit.DAYS)
            //.toInstant()
            //.atZone(ZoneId.systemDefault())
            //.toLocalDate();
        }

        List<String> nameList = departmentName.stream().toList();
        for (int i = 0; i < 30; i++) {
            String name = nameList.get(i);
            String description = departmentDescription.get(i);
            LocalDate date = establishedDate.get(i);
            Department department = new Department(name, description, date);
            departmentRepository.save(department);
        }
        em.flush();
        em.clear();
    }
}
