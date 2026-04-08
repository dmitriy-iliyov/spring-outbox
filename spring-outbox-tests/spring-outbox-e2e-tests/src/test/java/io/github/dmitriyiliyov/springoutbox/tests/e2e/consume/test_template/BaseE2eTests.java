package io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.test_template;

import io.github.dmitriyiliyov.springoutbox.tests.e2e.consume.ConsumeE2eTestApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestConstructor;

@SpringBootTest(classes = ConsumeE2eTestApplication.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseE2eTests { }