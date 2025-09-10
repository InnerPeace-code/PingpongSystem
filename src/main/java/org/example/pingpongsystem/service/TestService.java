package org.example.pingpongsystem.service;

import org.example.pingpongsystem.entity.testEntity;
import org.example.pingpongsystem.repository.TestRepository;
import org.springframework.stereotype.Service;

@Service
public class TestService {
    public void savePerson() {
        testEntity person = new testEntity();
        person.setName("林辉");
        person.setEmail("lmy_ethan@163.com");
        testRepository.save(person);
    }
    private final TestRepository testRepository;
    public TestService(TestRepository testRepository) {
        this.testRepository = testRepository;  // 由Spring容器注入实例
    }
}
