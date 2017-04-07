global.render = function () {
  return "This is rendered in JS"
};

global.renderPromise = function () {
  return {
    then: function (resolve) {
      setTimeout((function () {
        resolve("This is an async resolved String");
      }.bind({ someContext: "context" })), 100);
    }
  };
};