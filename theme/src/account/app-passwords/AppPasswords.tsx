import { useTranslation } from "react-i18next";
import { Page } from "../components/page/Page";
import {
    Alert,
    Button,
    ButtonVariant,
    capitalize,
    ClipboardCopy,
    DataList,
    DataListAction,
    DataListCell,
    DataListItem,
    DataListItemCells,
    DataListItemRow,
    Modal,
    ModalVariant,
    Text,
    TextContent
} from "@patternfly/react-core";
import { CSSProperties, useState } from "react";
import { useEnvironment } from "../../shared/keycloak-ui-shared";
import { appPasswordClient } from "./app-password-client";
import { Environment } from "../environment";
import { usePromise } from "../utils/usePromise";
import { TFunction } from "i18next";
import { formatDate } from "../utils/formatDate";

const appPasswordRowCells = (
    item: AppPasswordListRepresentation,
    t: TFunction<"translation", undefined>
) => {
    const maxWidth = {
        "--pf-v5-u-max-width--MaxWidth": "300px"
    } as CSSProperties;

    const items = [
        <DataListCell
            key="description"
            data-testrole="label"
            className="pf-v5-u-max-width"
            style={maxWidth}
        >
            {t(`appPasswordItem${capitalize(item.name)}`)}
        </DataListCell>,
        <DataListCell
            key="lastModified"
            data-testrole="label"
            className="pf-v5-u-max-width"
        >
            {!item.created && (
                <>
                    <strong>{t("appPasswordNotCreated")}</strong>
                </>
            )}
            {!!item.created && (
                <>
                    <strong>{t("appPasswordCreated")}</strong>{" "}
                    {formatDate(new Date(item.created))}
                </>
            )}
        </DataListCell>
    ];

    return items;
};

export const AppPasswords: React.FC = () => {
    const { t } = useTranslation();
    const context = useEnvironment<Environment>();
    const [appPasswords, setAppPasswords] = useState<AppPasswordListRepresentation[]>([]);
    const realm = context.keycloak.realm;
    const [generatedPassword, setGeneratedPassword] = useState<
        AppPasswordGenerateRepresentation | undefined
    >();
    const [deleteModal, setDeleteModal] = useState<boolean | string>(false);
    const [regenerateModal, setRegenerateModal] = useState<boolean | string>(false);

    usePromise(
        async signal => {
            if (!realm) {
                return [];
            }
            return appPasswordClient.list(signal, context);
        },
        async appPasswords => setAppPasswords(appPasswords),
        [realm]
    );

    const generateAppPassword = async (appPasswordName: string) => {
        if (!realm) {
            return;
        }
        const generatedPassword = await appPasswordClient.generate(
            context,
            appPasswordName
        );
        setGeneratedPassword(generatedPassword);
        const newAppPasswords = appPasswords
            .filter(x => x.name !== appPasswordName)
            .concat([
                {
                    name: generatedPassword.name,
                    created: generatedPassword.created
                }
            ]);
        setAppPasswords(newAppPasswords);
    };

    const deleteAppPassword = async (appPasswordName: string) => {
        if (!realm) {
            return;
        }

        await appPasswordClient.delete(context, appPasswordName);

        const newAppPasswords = appPasswords
            .filter(x => x.name !== appPasswordName)
            .concat([
                {
                    name: appPasswordName,
                    created: null
                }
            ]);
        setGeneratedPassword(undefined);
        setAppPasswords(newAppPasswords);
    };

    return (
        <Page title={t("appPasswords")} description={t("appPasswordsDescription")}>
            <DataList
                aria-label="app passwords list"
                className="pf-v5-u-mb-xl"
                data-testid="app-passwords-list"
            >
                {appPasswords
                    .sort((a, b) => a.name.localeCompare(b.name))
                    .map(item => (
                        <DataListItem
                            key={item.name}
                            aria-labelledby={`app-password-${item.name}`}
                            className="pf-v5-u-mb-md"
                        >
                            <DataListItemRow id={`app-password-${item.name}`}>
                                <DataListItemCells
                                    className="pf-v5-u-py-0"
                                    dataListCells={[
                                        ...appPasswordRowCells(item, t),
                                        <DataListAction
                                            key="action"
                                            id={`action-${item.name}`}
                                            aria-label={t(
                                                !!item.created
                                                    ? "appPasswordRegenerateLabel"
                                                    : "appPasswordGenerateLabel"
                                            )}
                                            aria-labelledby={`app-password-${item.name}`}
                                        >
                                            <Button
                                                variant="secondary"
                                                onClick={() => {
                                                    if (item.created) {
                                                        setRegenerateModal(item.name);
                                                    } else {
                                                        generateAppPassword(item.name);
                                                    }
                                                }}
                                                data-testrole="regenerate"
                                            >
                                                {t(
                                                    item.created
                                                        ? "appPasswordRegenerate"
                                                        : "appPasswordGenerate"
                                                )}
                                            </Button>
                                            <Button
                                                variant="danger"
                                                onClick={() => setDeleteModal(item.name)}
                                                isDisabled={!item.created}
                                                data-testrole="delete"
                                            >
                                                {t("delete")}
                                            </Button>
                                        </DataListAction>
                                    ]}
                                />
                            </DataListItemRow>
                            {!!generatedPassword &&
                                generatedPassword.name === item.name && (
                                    <Alert
                                        isInline
                                        title={t("appPasswordGeneratedTitle", {
                                            name: t(
                                                `appPasswordItem${capitalize(item.name)}`
                                            )
                                        })}
                                        variant="warning"
                                        actionLinks={
                                            <Button
                                                id="delete-account-btn"
                                                variant="danger"
                                                onClick={() =>
                                                    setGeneratedPassword(undefined)
                                                }
                                                className="delete-button"
                                            >
                                                {t("appPasswordClearGenerated")}
                                            </Button>
                                        }
                                    >
                                        <TextContent>
                                            <Text as="p">
                                                {t("appPasswordGeneratedDescription")}
                                            </Text>
                                            <ClipboardCopy
                                                isReadOnly
                                                variant="inline-compact"
                                            >
                                                {generatedPassword.password}
                                            </ClipboardCopy>
                                        </TextContent>
                                    </Alert>
                                )}
                        </DataListItem>
                    ))}
            </DataList>
            <Modal
                title={t("appPasswordRegenerateModal", {
                    name: !!regenerateModal
                        ? t(`appPasswordItem${capitalize(regenerateModal as string)}`)
                        : ""
                })}
                onClose={() => setRegenerateModal(false)}
                variant={ModalVariant.small}
                actions={[
                    <Button
                        key="confirm"
                        variant="warning"
                        data-testid="confirm"
                        onClick={() => {
                            generateAppPassword(regenerateModal as string);
                            setRegenerateModal(false);
                        }}
                    >
                        {t("appPasswordRegenerate")}
                    </Button>,
                    <Button
                        key="cancel"
                        data-testid="cancel"
                        variant={ButtonVariant.link}
                        onClick={() => setRegenerateModal(false)}
                    >
                        {t("cancel")}
                    </Button>
                ]}
                isOpen={!!regenerateModal}
            >
                {t("appPasswordRegenerateDescription")}
            </Modal>
            <Modal
                title={t("appPasswordDeleteModal", {
                    name: !!deleteModal
                        ? t(`appPasswordItem${capitalize(deleteModal as string)}`)
                        : ""
                })}
                onClose={() => setDeleteModal(false)}
                variant={ModalVariant.small}
                actions={[
                    <Button
                        key="delete"
                        variant="danger"
                        data-testid="confirm"
                        onClick={() => {
                            deleteAppPassword(deleteModal as string);
                            setDeleteModal(false);
                        }}
                    >
                        {t("delete")}
                    </Button>,
                    <Button
                        key="cancel"
                        data-testid="cancel"
                        variant={ButtonVariant.link}
                        onClick={() => setDeleteModal(false)}
                    >
                        {t("cancel")}
                    </Button>
                ]}
                isOpen={!!deleteModal}
            >
                {t("appPasswordDeleteDescription")}
            </Modal>
        </Page>
    );
};
