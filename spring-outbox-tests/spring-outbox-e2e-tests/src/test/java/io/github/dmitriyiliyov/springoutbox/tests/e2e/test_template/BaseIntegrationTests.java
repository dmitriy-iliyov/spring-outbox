package io.github.dmitriyiliyov.springoutbox.tests.e2e.test_template;

import io.github.dmitriyiliyov.springoutbox.tests.e2e.TestApplication;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class BaseIntegrationTests { }