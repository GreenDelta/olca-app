var olca = olca || {};

/** Splits a list into several pages. */
olca.Pager = function (pageSize) {

    this.pageSize = pageSize;
    this.currentPage = 0;
    this.pageContent = [];
    this.pageCount = 0;
    this.items = [];

    var self = this;

    /** Sets the items of the pager and selects the first page. */
    this.setItems = function (items) {
        self.items = items;
        self.currentPage = 1;
        self.pageCount = Math.ceil(items.length / self.pageSize);
        if (self.pageCount < 2) {
            self.pageCount = 1;
            self.pageContent = items;
        } else {
            self.selectPage(1);
        }
    };

    /** Selects the given page. */
    this.selectPage = function (number) {
        if (number > self.pageCount || number < 1) {
            return;
        }
        self.currentPage = number;
        self.pageContent = [];
        var begin = (number - 1) * self.pageSize,
            end = begin + self.pageSize;
        end = end > self.items.length ? self.items.length : end;
        for (var i = begin; i < end; i++) {
            self.pageContent.push(self.items[i]);
        }
    };

    /** Selects the next page. */
    this.next = function () {
        self.selectPage(self.currentPage + 1);
    };

    /** Selects the previous page. */
    this.previous = function () {
        self.selectPage(self.currentPage - 1);
    };
};

olca.range = function (start, end) {
    var ret = [];
    if (!end) {
        end = start;
        start = 0;
    }
    for (var i = start; i < end; i++) {
        ret.push(i);
    }
    return ret;
};

olca.max = function (collection, fn) {
    if (!collection)
        return null;
    var max = fn ? fn(collection[0]) : collection[0];
    for (var i = 1; i < collection.length; i++) {
        var next = fn ? fn(collection[i]) : collection[i];
        if (next > max)
            max = next;
    }
    return max;
};


olca.refValue = function (collection, fn) {
    if (!collection)
        return 0;
    var max = fn ? fn(collection[0]) : collection[0],
        min = max;
    for (var i = 1; i < collection.length; i++) {
        var next = fn ? fn(collection[i]) : collection[i];
        max = Math.max(max, next);
        min = Math.min(min, next);
    }
    return Math.max(Math.abs(max), Math.abs(min));
};

olca.contributionColor = function (datum, refAmount, fn) {
    if (refAmount === 0)
        return 0;
    var value = fn ? fn(datum) : datum;
    var share = Math.abs(value) / refAmount;
    var red, green, blue;
    if (value >= 0) {
        red = Math.round(255 * share);
        green = Math.round(Math.sqrt(red * (255 - red)));
        blue = Math.round(255 - red);
    } else {
        green = Math.round(255 * share);
        red = Math.round(Math.sqrt(green * (255 - green)));
        blue = Math.round(255 - green);
    }
    var redHex = red.toString(16),
        greenHex = green.toString(16),
        blueHex = blue.toString(16);
    redHex = redHex.length === 1 ? "0" + redHex : redHex;
    greenHex = greenHex.length === 1 ? "0" + greenHex : greenHex;
    blueHex = blueHex.length === 1 ? "0" + blueHex : blueHex;
    return "#" + redHex + greenHex + blueHex;
};