import React from "react";

import { ReportIndicator } from "./model";
import { isEmpty } from "./util";

export const IndicatorCombo = ({ indicators, selected, onChange }: {
    indicators: ReportIndicator[],
    selected: ReportIndicator,
    onChange: (next: ReportIndicator) => void,
}) => {

    if (isEmpty(indicators)) {
        return <></>;
    }

    const options = indicators.map(indicator => (
        <option
            key={indicator.impact.refId}
            value={indicator.impact.refId}>
            {indicator.impact.name}
        </option>
    ));

    return (
        <form>
            <fieldset>
                <select
                    value={selected?.impact.refId}
                    style={{ width: 300 }}
                    onChange={e => {
                        let selected: ReportIndicator = null;
                        const refId = e.target.value;
                        for (const i of indicators) {
                            if (i.impact.refId === refId) {
                                selected = i;
                                break;
                            }
                        }
                        if (selected) {
                            onChange(selected);
                        }
                    }}>
                    {options}
                </select>
            </fieldset>
        </form>
    );
}