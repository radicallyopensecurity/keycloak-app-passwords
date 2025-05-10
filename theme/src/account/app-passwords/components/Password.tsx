import { Button, Flex, FlexItem, Tooltip } from "@patternfly/react-core";
import { CopyIcon, EyeIcon, EyeSlashIcon } from "@patternfly/react-icons";
import React, { createRef, useState } from "react";
import { useTranslation } from "react-i18next";

const copyToClipboard = (text: string) => {
    try {
        navigator.clipboard.writeText(text.toString());
    } catch (error) {
        // eslint-disable-next-line no-console
        console.warn(
            "Clipboard API not found, this copy function will not work. This is likely because you're using an",
            "unsupported browser or you're not using HTTPS. \n\nIf you're a developer building an application which needs",
            "to support copying to the clipboard without the clipboard API, you'll have to create your own copy",
            "function and pass it to the ClipboardCopy component as the onCopy prop. For more information see",
            "https://developer.mozilla.org/en-US/docs/Web/API/Navigator/clipboard"
        );

        // eslint-disable-next-line no-console
        console.error(error);
    }
};

type PasswordProps = {
    value: string;
};

export const Password: React.FC<PasswordProps> = ({ value }) => {
    const copyRef = createRef<HTMLButtonElement>();
    const revealRef = createRef<HTMLButtonElement>();
    const [copied, setCopied] = useState(false);
    const [revealed, setRevealed] = useState(false);
    const { t } = useTranslation();

    const password = revealed ? value : "●".repeat(value.length);

    const RevealIcon = revealed ? EyeSlashIcon : EyeIcon;

    const clipboardText = copied
        ? t("appPasswordCopiedToClipboard")
        : t("appPasswordCopyToClipboard");

    const revealedText = revealed ? t("appPasswordHideText") : t("appPasswordRevealText");

    return (
        <Flex
            style={{
                padding: "var(--pf-v5-global--spacer--xs)",
                display: "inline-flex",
                background: "var(--pf-v5-global--BackgroundColor--200)"
            }}
            columnGap={{ default: "columnGapSm" }}
        >
            <FlexItem as="span" className="pf-v5-u-font-family-monospace">
                {password}
            </FlexItem>
            <Flex columnGap={{ default: "columnGapSm" }}>
                <Tooltip
                    trigger="mouseenter focus click"
                    triggerRef={revealRef}
                    content={revealedText}
                    exitDelay={1000}
                    entryDelay={300}
                >
                    <Button
                        ref={revealRef}
                        variant="plain"
                        style={{ padding: 0 }}
                        onClick={() => setRevealed(!revealed)}
                    >
                        <RevealIcon />
                    </Button>
                </Tooltip>
                <Tooltip
                    trigger="mouseenter focus click"
                    triggerRef={copyRef}
                    content={clipboardText}
                    exitDelay={1000}
                    entryDelay={300}
                    onTooltipHidden={() => {
                        setCopied(false);
                    }}
                >
                    <Button
                        variant="plain"
                        style={{ padding: 0 }}
                        ref={copyRef}
                        onClick={() => {
                            copyToClipboard(value);
                            setCopied(true);
                        }}
                    >
                        <CopyIcon />
                    </Button>
                </Tooltip>
            </Flex>
        </Flex>
    );
};
