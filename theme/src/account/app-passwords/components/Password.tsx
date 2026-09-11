import { Button, Flex, FlexItem, Tooltip } from "@patternfly/react-core";
import { CopyIcon, EyeIcon, EyeSlashIcon } from "@patternfly/react-icons";
import React, { useRef, useState } from "react";
import { useTranslation } from "react-i18next";

const copyToClipboard = async (text: string) => {
    try {
        await navigator.clipboard.writeText(text);
        return true;
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

        return false;
    }
};

const selectText = (element: HTMLElement | null) => {
    if (element === null) {
        return;
    }

    const selection = window.getSelection();

    if (selection === null) {
        return;
    }

    const range = document.createRange();
    range.selectNodeContents(element);

    selection.removeAllRanges();
    selection.addRange(range);
};

type PasswordProps = {
    value: string;
};

export const Password: React.FC<PasswordProps> = ({ value }) => {
    const copyRef = useRef<HTMLButtonElement>(null);
    const revealRef = useRef<HTMLButtonElement>(null);
    const passwordRef = useRef<HTMLSpanElement>(null);

    const [copyButtonCopied, setCopyButtonCopied] = useState(false);
    const [passwordCopied, setPasswordCopied] = useState(false);

    const [revealed, setRevealed] = useState(false);

    const { t } = useTranslation();

    const password = revealed ? value : "●".repeat(value.length);
    const RevealIcon = revealed ? EyeSlashIcon : EyeIcon;

    const revealedText = revealed ? t("appPasswordHideText") : t("appPasswordRevealText");

    const copyPassword = async () => {
        return copyToClipboard(value);
    };

    return (
        <Flex
            style={{
                padding: "var(--pf-v5-global--spacer--xs)",
                display: "inline-flex",
                background: "var(--pf-v5-global--BackgroundColor--200)"
            }}
            columnGap={{ default: "columnGapSm" }}
        >
            <FlexItem>
                <Tooltip
                    trigger="mouseenter focus click"
                    triggerRef={passwordRef}
                    content={
                        passwordCopied
                            ? t("appPasswordCopiedToClipboard")
                            : t("appPasswordCopyToClipboard")
                    }
                    exitDelay={1000}
                    entryDelay={300}
                    onTooltipHidden={() => setPasswordCopied(false)}
                >
                    <span
                        ref={passwordRef}
                        className="pf-v5-u-font-family-monospace"
                        data-testid="app-passwords-value"
                        style={{
                            cursor: "pointer",
                            userSelect: "all"
                        }}
                        onClick={async () => {
                            selectText(passwordRef.current);

                            if (await copyPassword()) {
                                setPasswordCopied(true);
                            }
                        }}
                    >
                        {password}
                    </span>
                </Tooltip>
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
                        data-testid="app-passwords-reveal"
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
                    content={
                        copyButtonCopied
                            ? t("appPasswordCopiedToClipboard")
                            : t("appPasswordCopyToClipboard")
                    }
                    exitDelay={1000}
                    entryDelay={300}
                    onTooltipHidden={() => setCopyButtonCopied(false)}
                >
                    <Button
                        variant="plain"
                        style={{ padding: 0 }}
                        ref={copyRef}
                        data-testid="app-passwords-copy"
                        onClick={async () => {
                            if (await copyPassword()) {
                                setCopyButtonCopied(true);
                            }
                        }}
                    >
                        <CopyIcon />
                    </Button>
                </Tooltip>
            </Flex>
        </Flex>
    );
};
