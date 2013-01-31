var express = require('express'),
    fs = require("fs"),
    app = express();
    
app.configure(function () {
    app.use(express.static(__dirname + '/public'));
     app.use(express.bodyParser());
});

var selected = {
    applications: 0,
    interviews: 0,
    shortlist: 0
};

var html = {
    applications: [
        "applications-none.html",
        "applications-no-active.html",
        "applications-both-employed.html",
        "applications-both-not-employed.html"
    ],
    interviews: [
        "interviews-none.html",
        "interviews-2jobs.html",
        "interviews-3jobs.html"
    ],
    shortlist: [
        "shortlist.html"
    ]
};

var cache = {};

app.get("/applications/", function(req, res){
    var file = "pages/" + html.applications[selected.applications];
    if (cache.applications) {
        var data = cache.applications[selected.applications];
        res.send(data);
    } else {
        res.send("Cannot send data, failed on server");
    }
});

app.get("/interviews/", function(req, res){
    var file = "pages/" + html.interviews[selected.interviews];
    if (cache.interviews) {
        var data = cache.interviews[selected.interviews];
        res.send(data);
    } else {
        res.send("Cannot send data, failed on server");
    }
});

app.get("/shortlist/", function(req, res){
    var file = "pages/" + html.shortlist[selected.shortlist];
    if (cache.shortlist) {
        var data = cache.shortlist[selected.shortlist];
        res.send(data);
    } else {
        res.send("Cannot send data, failed on server");
    }
});

app.post("/changepage/", function(req, res) {
    var p = req.body;
    if (selected.hasOwnProperty(p.page)) {
        selected[p.page] = p.number;
    }
    return res.json({
        success: true,
        data: {
            page: p.page,
            number: p.number
        },
    });
});

app.get("/selected/", function(req, res) {
    return res.json({
        success: true,
        data: selected
    });
});

// Read pages into cache
(function() {
    var queue = [];
    function readToCache() {
        if (queue.length > 0) {
            var item = queue.shift(),
                file = item.file,
                page = item.page,
                num = item.num;
            if (fs.existsSync(file)) {
                fs.readFile(file, function(e, data){
                    if (e) return console.dir(e);
                    data = data.toString();
                    if (!cache[page]) {
                        cache[page] = [];
                    }
                    cache[page].push(data);
                    readToCache();
                });
            } else {
                readToCache();
            }
        }
    }
    for (var page in html) {
        for (var i in html[page]) {
            var file = "pages/" + html[page][i];
            queue.push({file: file, page: page, num: i});
        }
    }
    readToCache();
})();

app.listen(1111);