<script>
window.onresize = function(){ window.location.href = window.location.href; }
var treeMap = new TreeMap();
treeMap.setRenderer(new CushionRectangleRenderer());
treeMap.setLayout(new SquarifiedLayout(10));
treeMap.addSelectionChangeListener({
  selectionChanged: function(aTreeMap, node) {
    var tm = aTreeMap.getTreeModel();
    if (tm) {
      var info = document.getElementById("treemap.info");
      if (info) {
        var labels = [];
        var stop = tm.getRoot();
        var runner = node;
        do {
          labels.unshift(tm.getLabel(runner));
          runner = treeModel.getParent(runner);
        } while (runner && runner != stop);
       
        info.innerHTML = labels.join("/");
      }
      info = document.getElementById("treemap.size");
      if (info) {
        info.innerHTML = tm.getWeight(node);
      }
    }
  }
});
treeMap.setColorProvider({
  getColor: function(node) {
    var result = "#204040";
    if (node.label) {
      @COLORS@
    }
    return result;
  }
});

var canvas = document.getElementById("treemap");
canvas.style.width = "100%";
canvas.style.height = "100%";
canvas.width  = canvas.offsetWidth;
canvas.height = Math.round(0.65 * window.innerHeight);
treeMap.hook(canvas);

var treeModel = new JSONTreeModel(@TREEMODEL@);
var total = document.getElementById("total");
if (total) {
  total.innerText = treeModel.getWeight(treeModel.getRoot());
}
treeMap.setTreeModel(treeModel);
treeMap.compute();
treeMap.paint();
</script>
