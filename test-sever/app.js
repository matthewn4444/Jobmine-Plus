var express = require('express'),
    cons = require("consolidate"),
    swig = require("swig"),
    fs = require("fs"),
    app = express();

app.engine('.html', cons.swig);
app.set('view engine', 'html');

swig.init({
    root: 'pages/',
    allowErrors: true
});
app.set('views', 'pages/');

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
        "applications-none",
        "applications-no-active",
        "applications-both-employed",
        "applications-both-not-employed"
    ],
    interviews: [
        "interviews-none",
        "interviews-2jobs",
        "interviews-3jobs"
    ],
    shortlist: [
        "shortlist",
        "shortlist-test"
    ]
};

var cachedData = {};

function getCachedData(path) {
    if (cachedData.hasOwnProperty(path)) {
        return cachedData[path];
    }
    var data;
    if (fs.existsSync(path + ".js")) {
        data = cachedData[path] = require(path);
    } else {
        data = cachedData[path] = 0;
    }
    return data;
}

app.get("/applications/", function(req, res){
    var file = html.applications[selected.applications];
    if (data = getCachedData("./data/" + file))
        res.render(file + ".html", {data: data.data});
    else 
        res.render(file + ".html");
});

app.get("/interviews/", function(req, res){
    var file = html.interviews[selected.interviews];
    if (data = getCachedData("./data/" + file))
        res.render(file + ".html", {data: data.data});
    else 
        res.render(file + ".html");
});

app.get("/shortlist/", function(req, res){
    var file = html.shortlist[selected.shortlist];
    if (data = getCachedData("./data/" + file))
        res.render(file + ".html", {data: data.data});
    else 
        res.render(file + ".html");
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

app.listen(1111);