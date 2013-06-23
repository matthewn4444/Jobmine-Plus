var express = require('express'),
    cons = require("consolidate"),
    swig = require("swig"),
    fs = require("fs"),
    app = express();

app.engine('.html', cons.swig);
app.set('view engine', 'html');

swig.init({
    root: 'pages/',
    allowErrors: true,
    filters: require("./swig/filters")
});
app.set('views', 'pages/');

app.configure(function () {
    app.use(express.static(__dirname + '/public'));
     app.use(express.bodyParser());
});


// Items
var selected = {};
var html = {};
var dataRender = {};

// Read the files inside "pages"
var files = fs.readdirSync("./pages/files");
for (var i = 0; i < files.length; i++) {
    var filename = files[i].substring(0, files[i].lastIndexOf(".")),
        firstHyphen = filename.indexOf("-"),
        category = filename.substr(0, firstHyphen),
        name = filename.substring(firstHyphen + 1);
    if (!html[category]) {  
        html[category] = [filename];
        selected[category] = 0;
        dataRender[category] = [name];
    } else {
        html[category].push(filename);
        dataRender[category].push(name);
    }
}

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

app.get("/", function(req, res){
    res.render("index.html", {data: dataRender});
});

function routePage(page) {
    app.get("/" + page + "/", function(req, res){
        var file = html[page][selected[page]];
        if (data = getCachedData("./data/" + file))
            res.render("files/" + file + ".html", {data: data.data});
        else 
            res.render("files/" + file + ".html");
    });
}

for (var i in dataRender) {
    if (dataRender.hasOwnProperty(i)) {
        routePage(i);
    }
}

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