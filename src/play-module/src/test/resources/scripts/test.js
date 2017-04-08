global.render = function () {
  return "This is rendered in JS"
};

global.renderPromise = function () {
  return {
    then: function (resolve) {
      setTimeout((function () {
        setTimeout(function(){
          resolve("This is an async resolved String");
        }, 20);
      }.bind({ someContext: "context" })), 100);
    }
  };
};