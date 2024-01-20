import React, { useState } from "react";
import { render } from "react-dom";

type PageData = {
  version: string;
  showLibHint: boolean;
};

enum Section {
  GETTING_STARTED,
  WHATS_NEW,
  DOCUMENTATION,
  COMMUNITY,
  DATABASES,
  TRAININGS,
  MORE_TOOLS,
  JOINING,
}


const Page = ({ data }: { data: PageData }) => {

  const sections = [
    Section.GETTING_STARTED,
    Section.WHATS_NEW,
    Section.DOCUMENTATION,
    Section.COMMUNITY,
    Section.DATABASES,
    Section.TRAININGS,
    Section.MORE_TOOLS,
    Section.JOINING,
  ];

  return (
    <div className="container-fluid">
      <div className="grid">
        <div />
        <div>
          <a href="http://www.openlca.org"
            title="http://www.openlca.org"
            className="openlca-logo">
            <img
              className="openlca-logo"
              src="images/logo_start_page.png" />
          </a>
        </div>
      </div>
      <div className="grid">
        <div>
          <Nav sections={sections} />
        </div>
        <div></div>
      </div>
    </div>
  );
};

const Nav = ({ sections }: { sections: Section[] }) => {
  const items = sections.map(s => {
    return <li className="olca-nav-item">{headerOf(s)}</li>
  });
  return <aside>
    <nav>
      <ul className="olca-nav">
        {items}
      </ul>
    </nav>
  </aside>
}

function headerOf(s: Section): string {
  switch (s) {
    case Section.GETTING_STARTED:
      return "Getting started";
    case Section.WHATS_NEW:
      return "What's new in openLCA 2";
    case Section.DOCUMENTATION:
      return "Extensive documentation";
    case Section.COMMUNITY:
      return "Community Q&A";
    case Section.DATABASES:
      return "Databases, case studies, impact methods";
    case Section.TRAININGS:
      return "Certified trainings, support";
    case Section.MORE_TOOLS:
      return "Tools to get more out of openLCA";
    case Section.JOINING:
      return "Working with the developers of openLCA";
    default:
      "ERROR: unknown section";
  }
}


// expose the setData function by binding it to the window object
// onOpenLink can be bound to an event handler (link: string) => void
declare global {
  interface Window {
    setData: any;
    onOpenLink: any;
    onLibHintClick: any;
  }
}

const setData = (data: PageData) => {
  render(<Page data={data} />, document.getElementById("app"));
};
window.setData = setData;

setData({
  version: "Version 2.1.0",
  showLibHint: true,
});


