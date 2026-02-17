package net.matsudamper.money.ui.root.platform

import net.matsudamper.money.frontend.common.ui.layout.image.SelectedImage

public interface ImagePicker {
    public suspend fun pickImage(): SelectedImage?
}
