import React, { useState, CSSProperties } from "react";
import { render } from "react-dom";

type PageData = {
  version: string;
  showLibHint: boolean;
};

enum Item {
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

  const items = [
    Item.GETTING_STARTED,
    Item.WHATS_NEW,
    Item.DOCUMENTATION,
    Item.COMMUNITY,
    Item.DATABASES,
    Item.TRAININGS,
    Item.MORE_TOOLS,
    Item.JOINING,
  ];

  const [selected, setSelected] = useState<Item>(Item.GETTING_STARTED);

  return (
    <div className="container-fluid">
      <div className="grid">
        <div />
        <div>
          <a href="http://www.openlca.org" className="openlca-logo">
            <img className="openlca-logo"
              src="images/logo_start_page.png" />
          </a>
        </div>
      </div>
      <div className="grid">
        <div>
          <Nav items={items} selected={selected} onSelect={setSelected} />
        </div>
        <div className="olca-content-container">
          <Content item={selected} />
        </div>
      </div>
    </div>
  );
};

const Nav = ({ items, selected, onSelect }: {
  items: Item[],
  selected: Item,
  onSelect: (i: Item) => void,
}) => {

  const elems = items.map(i => {
    const style: CSSProperties = i === selected
      ? { "textDecoration": "underline" }
      : {};
    /*
    if (i === Item.JOINING) {
        style["color"] = "#69558F";
    }
      */

    return (
      <li className="olca-nav-item" style={style} onClick={() => onSelect(i)}>
        {headerOf(i)}
      </li>
    );
  });

  return (
    <aside>
      <nav>
        <ul className="olca-nav">
          {elems}
        </ul>
      </nav>
    </aside>
  );
};

const Content = ({ item }: { item: Item }) => {
  switch (item) {

    case Item.GETTING_STARTED:
      return (
        <div className="olca-content">
          <p>
            With openLCA, you can model the life cycle of everything. Create or
            import existing databases with life cycle processes and import
            assessment methods; create your own processes, build your own life
            cycle models, calculate and analyse them. Keep reading to know more.
          </p>
        </div>
      );

    case Item.WHATS_NEW:
      return (
        <div className="olca-content">
          <p>
            openLCA 1 was already quite a good software, we made it even better
            (more modelling options, faster calculation, better usability including
            better design, enhanced quality assurance support, etc.). Check the
            <a href="https://www.openlca.org/openlca-2-0-is-now-available-for-download/"> blogpost </a>
            to see what we added and improved in version 2.
          </p>
          <p>
            openLCA is updated regularly, latest versions are available from the
            <a href="https://www.openlca.org/download/"> openLCA download page</a>.
          </p>
        </div>
      );

    case Item.DOCUMENTATION:
      return (
        <div className="olca-content">
          <p>
            There are many
            <a href="https://www.openlca.org/learning/"> resources </a>
            for openLCA: the
            <a href="https://manuals.openlca.org/openlca/"> manual</a>,
            <a href="https://www.openlca.org/case-studies/"> case studies</a>,
            our
            <a href="https://www.youtube.com/@openLCA"> YouTube channel </a>
            with a lot of learning and informative material, a
            <a href="https://ask.openlca.org/"> forum</a>.
            We also offer
            <a href="https://www.openlca.org/trainings/"> trainings</a>.
          </p>
        </div>
      );

    case Item.COMMUNITY:
      return (
        <div className="olca-content">
          <p>
            If you run into a problem or have a question, maybe someone had a
            similar issue to yours. For that, we have an ask forum for openLCA
            community question and answers:
            <a href="https://ask.openlca.org/"> https://ask.openlca.org/</a>
          </p>
        </div>
      );

    case Item.DATABASES:
      return (
        <div className="olca-content">
          <p>
            Since more than 10 years we run
            <a href="https://nexus.openlca.org/"> openLCA Nexus</a>,
            the source for LCA and sustainability datasets, databases, case
            studies and LCA models for openLCA and other tools.
          </p>
        </div>
      );

    case Item.TRAININGS:
      return (
        <div className="olca-content">
          <p>
            Trainings on LCA, social LCA, sustainability and of course openLCA
            are available on a regular basis, provided by GreenDelta and also
            by other <a href="https://www.openlca.org/certified-trainers/">certified trainers</a> worldwide.
            They are posted and can be <a href="https://nexus.openlca.org/service/openLCA%20Trainings">booked on Nexus</a>.
          </p>
          <p>
            Software and modelling support is available as well, it can
            be <a href="https://nexus.openlca.org/service/openLCA%20Support%20(help%20desk)">booked via Nexus</a>,
            contact us for larger packs or inquiries.
          </p>
        </div>
      );

    case Item.MORE_TOOLS:
      return (
        <div className="olca-content">
          <p>
            The LCA Collaboration Server is a unique tool to bring the
            professionalism of distributed software code development into LCA
            data management, developed by GreenDelta since more than 5 years,
            with data review, detailed version control, publication
            possibilities. Find the manual <a href="https://manuals.openlca.org/lca-collaboration-server/">here</a>, some public
            cases are listed <a href="https://www.lcacommons.gov/lca-collaboration/">here</a>.
          </p>
          <p>
            OnlineLCA - the webtool based on openLCA. More details here.
          </p>
        </div>
      );

    case Item.JOINING:
      return (
        <div className="olca-content">
          <p>
            openLCA is developed by GreenDelta in Berlin, Germany. If you're
            passionate about making a positive impact through your work - whether
            in IT development, data development, research, or consultancy - GreenDelta
            offers various open positions (available in German or English). We
            also welcome applications for internships. Check out
            our <a href="https://www.greendelta.com/about-us/open-positions/">current opportunities</a>.
          </p>
        </div>
      );

    default:
      return <></>;
  }
};

function headerOf(i: Item): string {
  switch (i) {
    case Item.GETTING_STARTED:
      return "Getting started";
    case Item.WHATS_NEW:
      return "What's new in openLCA 2";
    case Item.DOCUMENTATION:
      return "Extensive documentation";
    case Item.COMMUNITY:
      return "Community Q&A";
    case Item.DATABASES:
      return "Databases, case studies, impact methods";
    case Item.TRAININGS:
      return "Certified trainings, support";
    case Item.MORE_TOOLS:
      return "Tools to get more out of openLCA";
    case Item.JOINING:
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


