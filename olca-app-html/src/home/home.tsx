import React, { useState, useEffect, CSSProperties } from "react";
import { render } from "react-dom";
import messages from "./messages";

type Data = {
    version: string;
    lang: string;
};

type Props = {
    data: Data;
    messages: Record<string, string>;
};

const Page = (props: Props) => {

    const [openedBlock, setOpenedBlock] = useState<string | null>(null);
    const onBlockClicked = (id: string) => {
        if (id === openedBlock) {
            setOpenedBlock(null);
        } else {
            setOpenedBlock(id);
        }
    };

    useEffect(() => {
        bindLinks();
    });

    const blockConfig = [
        ["whats-new", "whatsNew"],
        ["getting-started", "gettingStarted"],
        ["manuals", "manuals"],
        ["community", "community"],
        ["experts", "experts"],
    ];
    const blocks = blockConfig.map(([className, msgKey]) => (
        <Block
            open={className === openedBlock}
            key={className}
            className={className}
            msgKey={msgKey}
            messages={props.messages}
            onClick={onBlockClicked} />
    ));
    return (
        <>
            <Header {...props} />
            {blocks}
            <div className="placeholder" />
            <a className="gd-logo"
                href="http://www.greendelta.com"
                title="http://www.greendelta.com" />
        </>
    );
};

const Header = ({ data, messages }: Props) => (
    <header>
        <a href="http://www.openlca.org" title="http://www.openlca.org">
            <img className="logo" src="images/logo_start_page.png" />
        </a>
        <div className="header-title">
            <div className="subtext">
                <div>{messages["title1"]}</div>
                <div>{messages["title2"]}</div>
                <div>{messages["title3"]}</div>
                <div>{messages["title4"]}</div>
            </div>
            <h2 id="version">{data.version}</h2>
        </div>
    </header>
);

type BlockProps = {
    className: string;
    msgKey: string;
    messages: Record<string, string>;
    open: boolean;
    onClick: (id: string) => void;
};

const Block = (props: BlockProps) => {

    const elemStyle: CSSProperties = {
        marginBottom: 50,
        width: props.open ? "100%" : "auto",
    };
    if (!props.open) {
        elemStyle.display = "inline-block";
    }
    const content = props.messages[props.msgKey + ".text"] + " " +
        props.messages[props.msgKey + ".link"];

    return (
        <div>
            <div className={"block " + props.className + (props.open ? " expanded" : "")}
                style={elemStyle}
                onClick={() => props.onClick(props.className)}>
                <span className="block-content">
                    <span className="title"
                        style={props.open ? { display: "none" } : { display: "inline" }}>
                        {props.messages[`${props.msgKey}.title`]}
                        <img className="chevron-right" src="images/chevron_right.png" />
                    </span>
                    <span className="content"
                        style={props.open ? { display: "inline" } : { display: "none" }}>
                        <span
                            dangerouslySetInnerHTML={{ __html: content }} />
                    </span>
                </span>
            </div>
        </div>
    );
};

const bindLinks = () => {
    const config = [
        ["news", "http://www.openlca.org/new"],
        ["video", "https://www.youtube.com/watch?v=FqVMbwhAEW4"],
        ["channel", "https://www.youtube.com/c/openLCA"],
        ["forum", "http://ask.openlca.org/"],
        ["linkedin", "https://www.linkedin.com/showcase/openlca/"],
        ["manuals", "http://www.openlca.org/learning"],
        ["greendelta", "http://www.greendelta.com"],
        ["github", "http://www.greendelta.com"],
        ["twitter", "https://twitter.com/openLCA"],
        ["blog", "http://www.openlca.org/blog/"],
        ["dataopenlca", "https://data.openlca.org/"],
        ["trainings", "https://www.openlca.org/trainings/"],
        ["services", "https://www.openlca.org/service-contracts/"],
        ["lcacs", "https://www.openlca.org/collaboration-server/"],
        ["hosting", "https://www.openlca.org/lca-collaboration-server-hosting-and-services/"],
    ];
    config.forEach(([id, link]) => {
        const elem = document.getElementById(id);
        if (!elem) {
            return;
        }
        if (elem.getAttribute("href")) {
            // avoid adding multiple event handlers
            return;
        }
        elem.setAttribute("href", link);
        elem.setAttribute("title", link);
        elem.addEventListener("click", (e) => {
            if (window.onOpenLink) {
                e.preventDefault();
                window.onOpenLink(link);
            }
        });
    });
};

const setData = (data: Data) => {
    render(<Page data={data} messages={messages[data.lang]} />,
        document.getElementById("react-root"));
};

setData({
    version: "Version 1.9.0",
    lang: "en",
});

// expose the setData function by binding it to the window object
// onOpenLink can be bound to an event handler (link: string) => void
declare global {
    interface Window {
        setData: any;
        onOpenLink: any;
    }
}
window.setData = setData;
