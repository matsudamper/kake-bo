package net.matsudamper.money.backend.graalvm;

import org.graalvm.nativeimage.hosted.Feature;

import java.util.List;

/**
 * native-image ビルド時にクラスパスを走査し、指定パッケージ配下の全クラスをリフレクション登録する Feature の基底クラス。
 * <p>
 * reflect-config.json はクラス名のワイルドカード指定ができないため、パッケージ単位で登録したい場合に使う。
 */
public abstract class PackageReflectionFeature implements Feature {

    protected abstract String targetPackage();

    /**
     * targetPackage 以外も同じ走査で登録したいときだけ上書きする。
     */
    protected List<String> extraPackages() {
        return List.of();
    }

    protected abstract void register(Class<?> clazz);

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        registerPackage(targetPackage());
        for (String extraPackage : extraPackages()) {
            registerPackage(extraPackage);
        }
    }

    private void registerPackage(String targetPackage) {
        List<String> classNames = PackageClassScanner.findClassNames(targetPackage);
        // 走査に失敗しても native-image のビルドは通り、実行時に初めて落ちるので、ここで止める
        if (classNames.isEmpty()) {
            throw new IllegalStateException(targetPackage + " にクラスが 1 つも無い。パッケージを移したか、走査に失敗している");
        }
        for (String className : classNames) {
            try {
                register(Class.forName(className));
            } catch (ClassNotFoundException e) {
                System.err.println("[" + getClass().getSimpleName() + "] Class not found: " + className);
            }
        }
    }
}
