package io.jd.framework;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;

public class ProcessingEnvUtils {
    private ProcessingEnvUtils() {
    }

    public static String getPackageName(ProcessingEnvironment processingEnvironment, Element element) {
        return getPackageElement(processingEnvironment, element).getQualifiedName().toString();
    }

    public static PackageElement getPackageElement(ProcessingEnvironment processingEnvironment, Element element) {
        return processingEnvironment.getElementUtils().getPackageOf(element);
    }

}
