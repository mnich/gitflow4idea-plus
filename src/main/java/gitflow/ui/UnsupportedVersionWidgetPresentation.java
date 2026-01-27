package gitflow.ui;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.MouseEvent;

public class UnsupportedVersionWidgetPresentation implements StatusBarWidget.TextPresentation {

	@NotNull
	@Override
	public String getText() {
		return "Unsupported Git Flow Version";
	}

	@Override
	public float getAlignment() {
		return 0;
	}

	@Nullable
	@Override
	public String getTooltipText() {
		return "Click for details";
	}

	@Nullable
	@Override
	public Consumer<MouseEvent> getClickConsumer() {
		return mouseEvent -> {
			MessageDialogBuilder.YesNo builder = MessageDialogBuilder.yesNo(
					"Unsupported Git Flow version",
					"The Git Flow CLI version installed isn't supported.\n\n" +
					"Supported implementations:\n" +
					"• git-flow (AVH Edition)\n" +
					"• git-flow-next\n" +
					"• gitflow-cjs\n\n" +
					"Please install one of the above.")
					.yesText("More information (open browser)")
					.noText("Close");
			if (builder.ask(mouseEvent.getComponent())) {
				BrowserUtil.browse("https://github.com/RubinCarter/gitflow4idea-plus/blob/develop/GITFLOW_VERSION.md");
			}
		};

	}
}
