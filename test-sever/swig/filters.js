exports.presentable = function(text) {
    text = text.toLowerCase().replace(/-/g," ");
    return text[0].toUpperCase() + text.substr(1);
}