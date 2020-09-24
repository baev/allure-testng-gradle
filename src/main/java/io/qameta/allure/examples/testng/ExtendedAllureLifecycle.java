package io.qameta.allure.examples.testng;

import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.AllureResultsWriter;
import io.qameta.allure.aspects.AttachmentsAspects;
import io.qameta.allure.aspects.StepsAspects;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.ExecutableItem;
import io.qameta.allure.model.Status;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
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

    static {
        try {
            final Field lifecycle = FieldUtils.getDeclaredField(Allure.class, "lifecycle", true);
            FieldUtils.writeStaticField(lifecycle, getLifecycle(), true);
            AttachmentsAspects.setLifecycle(ExtendedAllureLifecycle.getLifecycle());
            StepsAspects.setLifecycle(ExtendedAllureLifecycle.getLifecycle());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Writes test case with given uuid using configured {@link AllureResultsWriter}.
     *
     * @param uuid the uuid of test case to write.
     */
    public void writeTestCase(final String uuid) {
        updateTestCase(uuid, (testResult -> {
            if (Objects.nonNull(testResult.getStatus()) && testResult.getStatus().equals(Status.PASSED)) {
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

    private void writeAttachments(final ExecutableItem result) {
        if (Objects.nonNull(result.getAttachments())) {
            result.getAttachments().forEach(this::writeAttachmentFromFile);
        }
        if (Objects.nonNull(result.getSteps())) {
            result.getSteps().forEach(this::writeAttachments);
        }
    }

    private void writeAttachmentFromFile(final Attachment attachment) {
        final Path file = attachments.get(attachment.getSource());
        try (final InputStream stream = new FileInputStream(file.toFile())) {
            super.writeAttachment(attachment.getSource(), stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        };
    }

    private void cleanAttachments(final ExecutableItem result) {
        Optional.ofNullable(result.getAttachments()).ifPresent(List::clear);
        if (Objects.nonNull(result.getSteps())) {
            result.getSteps().forEach(this::cleanAttachments);
        }
    }

    public static AllureLifecycle getLifecycle() {
        if (Objects.isNull(lifecycle)) {
            lifecycle = new ExtendedAllureLifecycle();
        }
        return lifecycle;
    }

}
