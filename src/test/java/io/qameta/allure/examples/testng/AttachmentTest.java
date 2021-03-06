package io.qameta.allure.examples.testng;

import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.testng.annotations.Test;

public class AttachmentTest {

    @Test
    public void testAttachments() {
        attachment();
        doSomething();
        Allure.attachment("From Allure", "world");
        throw new RuntimeException("asd");
    }

    @Step
    private void doSomething() {
        attachment();
    }

    @Attachment(value = "hello", fileExtension = "txt")
    private byte[] attachment() {
        return "hello".getBytes();
    }

}
