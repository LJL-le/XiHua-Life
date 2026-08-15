Vue.component("footBar", {
  template: `
    <div class="foot">
      <div class="foot-box" :class="{active: activeBtn === 1}" @click="toPage(1)">
        <div class="foot-view"><i class="el-icon-s-home"></i></div>
        <div class="foot-text">&#39318;&#39029;</div>
      </div>
      <div class="foot-box" :class="{active: activeBtn === 2}" @click="toPage(2)">
        <div class="foot-view"><i class="el-icon-map-location"></i></div>
        <div class="foot-text">&#26657;&#22253;</div>
      </div>
      <div class="foot-box" @click="toPage(0)">
        <img class="add-btn" src="/imgs/add.png" alt="">
      </div>
      <div class="foot-box" :class="{active: activeBtn === 3}" @click="toPage(3)">
        <div class="foot-view"><i class="el-icon-chat-dot-round"></i></div>
        <div class="foot-text">&#28040;&#24687;</div>
      </div>
      <div class="foot-box" :class="{active: activeBtn === 4}" @click="toPage(4)">
        <div class="foot-view"><i class="el-icon-user"></i></div>
        <div class="foot-text">&#25105;&#30340;</div>
      </div>
    </div>
  `,
  props: ['activeBtn'],
  methods: {
    toPage(i) {
      if (i === 0) {
        location.href = "/blog-edit.html";
      } else if (i === 4) {
        location.href = "/info.html";
      } else if (i === 3) {
        location.href = "/message.html";
      } else if (i === 2) {
        location.href = "/shop-list.html";
      } else if (i === 1) {
        location.href = "/";
      }
    }
  }
});
