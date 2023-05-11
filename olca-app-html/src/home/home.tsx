import React, { useState, useEffect, CSSProperties } from 'react';
import { render } from 'react-dom';

type Data = {
    version: string;
    lang: string;
    showLibHint: boolean;
};

type Props = {
    data: Data;
};


const NAV_CONFIGURATION = [
    {
        navLabel: 'Getting Started',
        navId: 'getting-started',
    },
    {
        navLabel: "What's new in openLCA 2.0",
        navId: 'whats-new',
    },
    {
        navLabel: 'Collaboration tool for openLCA ',
        navId: 'collaboration-tool',
    },

    {
        navLabel: 'Community forum',
        navId: 'community-forum',
    },
    {
        navLabel: 'Comprehensive database',
        navId: 'databases',
    },
    {
        navLabel: 'Case studies',
        navId: 'case-studies',
    },
    {
        navLabel: 'Certified trainings',
        navId: 'certified-training',
    },
    {
        navLabel: 'Work with openLCA experts',
        navId: 'experts',
    },

];

type SupportedLanguages = 'en' | 'de';

const messages: {
    [k in SupportedLanguages]?: {
        [key: string]: string;
    };
} = {
    en: {
        'getting-started.text':
            "<a id=\"openlca\">openLCA</a> is a powerful, "
            + "<a id=\"opensource\">open source</a>, feature-rich software for "
            + "LCA and Sustainability modelling. "
            + "\nCreate, import existing databases which contain life cycle "
            + "processes, import assessment methods, create your own "
            + "processes, build your own life cycle models, calculate and "
            + "analyse it. These steps are explained on "
            + "<a id=\"channel\">YouTube</a>, and in the openLCA "
            + "<a id=\"manuals\">handbook</a>.",
        'whats-new.text':
            "openLCA 2 is a major step forward, with a lot of new features and "
            + "usability improvements. \n"
            + "New features include: new maps and "
            + "better regionalised modelling, broader support for various data "
            + "formats, natural modelling on an LCA canvas, libraries, EPDs "
            + "and results as new elements. For usability, the former model "
            + "graph and Sankey diagram are completely redesigned, many "
            + "editors have been improved, openLCA can now run in dark mode "
            + "and the installation on Mac is easier.",
        'community-forum.text':
            "Visit <a id=\"forum\">ask.openLCA.org</a> for questions and "
            + "answers around openLCA",
        'collaboration-tool.text':
            "The <a id=\"collaboration\">LCA Collaboration Server</a> is "
            + "developed for team work in LCA modelling, dataset "
            + "review and distribution. It is very similar to software code "
            + "development and it is inspired by the world-known Git software. "
            + "It is available for free, on the "
            + "<a id=\"collaboration-download\">openLCA website</a>. "
            + "GreenDelta also offers support and "
            + "<a id=\"collaboration-hosting\">hosting services</a>.",
        'databases.text':
            "Find a wide range of free and for-purchase databases for LCA and "
            + "sustainability modelling on "
            + "<a id=\"nexus\">openLCA Nexus</a>, which currently boasts "
            + "several hundred thousand datasets. If you have data that you "
            + "would like to share with other users, either for free or for a "
            + "fee, please do not hesitate to contact us. We would be more "
            + "than happy to help you make your valuable contribution "
            + "accessible to a broader audience.",
        'case-studies.text':
            "<a id=\"nexus\">Nexus</a> houses a repository of "
            + "<a id=\"casestudies\">case studies</a> comprising full openLCA "
            + "models and accompanying reports for documentation purposes."
            + "\nContact us if you like to share your case study, either for "
            + "free or a fee.",
        'certified-training.text':
            "Trainings on LCA, social LCA, Sustainability and of course "
            + "openLCA are available on a regular basis, provided by "
            + "GreenDelta and also by other certified trainers worldwide. They "
            + "are posted and can be booked on "
            + "<a id=\"trainings\">Nexus</a>.",
        'experts.text':
           "openLCA is developed by <a id=\"greendelta\">GreenDelta</a> in "
           + "Berlin, Germany. If you're passionate about making a positive "
           + "impact through your work - whether it's in IT development, data "
           + "development, research, or consultancy - GreenDelta offers "
           + "various open positions (available in German or English). We also "
           + "welcome applications for internships. Check out our current "
           + "opportunities <a id=\"openpositions\">here</a>.",
    },
};

const getMessage = (
    key: string,
    lang: SupportedLanguages = 'en'
): string | undefined => {
    const langMessages = messages[lang] || messages['en'];
    return langMessages[key];
};

const LibHint = (props: Props) => {
    if (!props || !props.data || !props.data.showLibHint) {
        return <></>;
    }
    const handleClick = () => {
        if (window.onLibHintClick) {
            window.onLibHintClick();
        }
    };
    return (
        <p className="nav-info" onClick={() => handleClick()}>
            <img className="info-icon" src="images/info-32.png"></img>
            Make the calculations in openLCA faster. Click here to know more.
        </p>
    );
};

const Navigation = (props: Props) => {
    return (
        <div className="navigation">
            <a
                href="http://www.openlca.org"
                title="http://www.openlca.org"
                className="img-link"
            >
                <img
                    className="openlca-logo"
                    src="images/logo_start_page.png"
                />
            </a>
            <div className="nav-info-container">
                <LibHint {...props} />
            </div>
        </div>
    );
};

const LeftSection = (props: {
    activeMenu: string;
    setActiveMenu: (value: string) => void;
}) => {
    return (
        <nav className="nav-container">
            <ul className="nav">
                {NAV_CONFIGURATION.map((navItem, index) => (
                    <li
                        key={index}
                        className={`nav-item ${
                            props.activeMenu == navItem.navId ? 'active' : ''
                        }`}
                        onClick={() => props.setActiveMenu(navItem.navId)}
                    >
                        {' '}
                        {navItem.navLabel}
                    </li>
                ))}
            </ul>
        </nav>
    );
};

const RightSection = (props: { activeMenu: string }) => {
    const content = getMessage(`${props.activeMenu}.text`);
    return (
        <div className="right-section-container">
            <div className="content-box">
                <p>
                    <span dangerouslySetInnerHTML={{ __html: content }} />
                </p>
            </div>
        </div>
    );
};

const Footer = () => {
    return (
        <div className="footer-container">
            <a
                className="gd-logo"
                href="http://www.greendelta.com"
                title="http://www.greendelta.com"
            />
        </div>
    );
};

const Page = (props: Props) => {
    const [activeMenu, setActiveMenu] = useState('getting-started');

    useEffect(() => {
        bindLinks();
    });

    return (
        <div className="container">
            <div className="max-width-container">
                <header className="header">
                    <Navigation {...props} />
                </header>
                <section className="section">
                    <section className="left-section">
                        <LeftSection
                            activeMenu={activeMenu}
                            setActiveMenu={(value: string) =>
                                setActiveMenu(value)
                            }
                        />
                    </section>
                    <section className="right-section">
                        <RightSection activeMenu={activeMenu} />
                    </section>
                </section>
                <footer className="footer">
                    <Footer />
                </footer>
            </div>
        </div>
    );
};

const bindLinks = () => {
    const config = [
        ['openlca', 'https://www.openlca.org/'],
        ['news', 'https://www.openlca.org/new'],
        ['opensource', 'https://www.openlca.org/open-source/'],
        ['nexus', 'https://nexus.openlca.org/'],
        ['channel', 'https://www.youtube.com/c/openLCA'],
        ['forum', 'https://ask.openlca.org/'],
        ['manuals', 'https://www.openlca.org/learning'],
        ['greendelta', 'https://www.greendelta.com'],
        ['twitter', 'https://twitter.com/openLCA'],
        ['blog', 'https://www.openlca.org/blog/'],
        ['trainings', 'https://nexus.openlca.org/service/openLCA%20Trainings'],
        ['services', 'https://www.openlca.org/helpdesk'],
        ['collaboration', 'https://www.openlca.org/collaboration-server/'],
        ['collaboration-download', 'https://www.openlca.org/download/'],
        [
            'collaboration-hosting',
            'https://www.openlca.org/lca-collaboration-server-hosting-and-services/'
        ],
        ['casestudies','https://www.openlca.org/case-studies/'],
        [
            'hosting',
            'https://www.openlca.org/lca-collaboration-server-hosting-and-services/',
        ],
        ['openpositions','https://www.greendelta.com/about-us/open-positions']
    ];
    config.forEach(([id, link]) => {
        const elem = document.getElementById(id);
        if (!elem) {
            return;
        }
        if (elem.getAttribute('href')) {
            // avoid adding multiple event handlers
            return;
        }
        elem.setAttribute('href', link);
        elem.setAttribute('title', link);
        elem.addEventListener('click', (e) => {
            if (window.onOpenLink) {
                e.preventDefault();
                window.onOpenLink(link);
            }
        });
    });
};

const setData = (data: Data) => {
    render(<Page data={data} />, document.getElementById('react-root'));
};

setData({
    version: 'Version 1.9.0',
    lang: 'en',
    showLibHint: false,
});

// expose the setData function by binding it to the window object
// onOpenLink can be bound to an event handler (link: string) => void
declare global {
    interface Window {
        setData: any;
        onOpenLink: any;
        onLibHintClick: any;
    }
}
window.setData = setData;
