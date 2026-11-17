package net.matsudamper.money.frontend.common.base.image

/**
 * ドロップされた画像。ドロップ時点では読み込まず、受け取った側で [read] する。
 */
public interface DroppedImages {
    public suspend fun read(): List<SelectedImage>
}
