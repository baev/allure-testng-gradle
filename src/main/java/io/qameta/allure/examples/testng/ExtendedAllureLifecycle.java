package io.qameta.allure.examples.testng;

import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.AllureResultsWriter;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ExtendedAllureLifecycle extends AllureLifecycle {

    private static AllureLifecycle lifecycle;

    private final Map<String, Path> attachments = new ConcurrentHashMap<>();

    /**
     * Writes test case with given uuid using configured {@link AllureResultsWriter}.
     *
     * @param uuid the uuid of test case to write.
     */
    public void writeTestCase(final String uuid) {
        updateTestCase(uuid, (testResult -> {
            if (testResult.getStatus().equals(Status.PASSED)) {
                cleanAttachments(testResult);
            } else {
                writeAttachments(testResult);
            }
        }));
        super.writeTestCase(uuid);
    }

    /**
     * Writes test container with given uuid.
     *
     * @param uuid the uuid of container.
     */
    public void writeTestContainer(final String uuid) {
        super.writeTestContainer(uuid);
    }

    /**
     * Writes attachment with specified source.
     *
     * @param attachmentSource the source of attachment.
     * @param stream           the attachment content.
     */
    public void writeAttachment(final String attachmentSource, final InputStream stream) {
        try {
            final Path tmpAttachment = Files.createTempFile("allure", attachmentSource);
            try (OutputStream out = new FileOutputStream(tmpAttachment.toFile())) {
                IOUtils.copy(stream, out);
            }
            attachments.put(attachmentSource, tmpAttachment);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeAttachments(final TestResult result) {
        if (Objects.nonNull(result.getAttachments())) {
            for (final Attachment attachment: result.getAttachments()) {
                final String source = attachment.getSource();
                final Path file = attachments.get(source);
                try (final InputStream stream = new FileInputStream(file.toFile())) {
                    super.writeAttachment(source, stream);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                };
            }
        }
    }

    private void cleanAttachments(final TestResult result) {
        Optional.ofNullable(result.getAttachments()).ifPresent(List::clear);
        if (Objects.nonNull(result.getSteps())) {
            result.getSteps().forEach(this::cleanAttachments);
        }
    }

    private void cleanAttachments(final StepResult step) {
        Optional.ofNullable(step.getAttachments()).ifPresent(List::clear);
        if (Objects.nonNull(step.getSteps())) {
            step.getSteps().forEach(this::cleanAttachments);
        }
    }

    public static AllureLifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = new ExtendedAllureLifecycle();
        }
        return lifecycle;
    }

}
