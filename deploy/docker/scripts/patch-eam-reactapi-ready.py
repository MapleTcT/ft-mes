#!/usr/bin/env python3
from __future__ import annotations

import argparse
from pathlib import Path


DOCKER_DIR = Path(__file__).resolve().parents[1]
PROJECT_ROOT = DOCKER_DIR.parents[1]

TARGETS = (
    "greenDill/static/EAM/baseInfo/baseInfo/eamViewLayout/body.js",
    "greenDill/static/EAM/baseInfo/baseInfo/eamViewLayout/body-es5.js",
)

OLD_BODY_JS = """



    setTimeout(function () {
      //类型
\t\tvar labelText=ReactAPI.international.getText("EAM.propertyshowName.randon1572243037103.flag");
\t//区域位置
\t\tvar areaOption=ReactAPI.international.getText("EAM.baseInfo.BaseInfo.installPlace");
\t//设备类型
\t\tvar eamTypeOption=ReactAPI.international.getText("EAM.baseInfo.BaseInfo.eamType");

    $('div[name-key="EAM_1.0.0_baseInfo_eamViewLayout_baseInfo_nt"]').before(`
            <div style="text-align: center">${labelText}:  <select onchange=treeChangeView(this.value)  style="border:solid 1px;margin-top:10px" >
                        <option value="1">${areaOption}</option>
                        <option value="2">${eamTypeOption}</option>
                       </select>
            </div>`);
},1000)
"""

NEW_BODY_JS = """


function runWhenReactAPIReady(callback, retry) {
    retry = retry || 0;
    if (window.ReactAPI && ReactAPI.international && window.$) {
        callback();
        return;
    }
    if (retry < 50) {
        setTimeout(function () {
            runWhenReactAPIReady(callback, retry + 1);
        }, 200);
    }
}

runWhenReactAPIReady(function () {
      //类型
\t\tvar labelText=ReactAPI.international.getText("EAM.propertyshowName.randon1572243037103.flag");
\t//区域位置
\t\tvar areaOption=ReactAPI.international.getText("EAM.baseInfo.BaseInfo.installPlace");
\t//设备类型
\t\tvar eamTypeOption=ReactAPI.international.getText("EAM.baseInfo.BaseInfo.eamType");

    $('div[name-key="EAM_1.0.0_baseInfo_eamViewLayout_baseInfo_nt"]').before(`
            <div style="text-align: center">${labelText}:  <select onchange=treeChangeView(this.value)  style="border:solid 1px;margin-top:10px" >
                        <option value="1">${areaOption}</option>
                        <option value="2">${eamTypeOption}</option>
                       </select>
            </div>`);
})
"""

OLD_BODY_ES5 = """

setTimeout(function () {
    //类型
    var labelText = ReactAPI.international.getText("EAM.propertyshowName.randon1572243037103.flag");
    //区域位置
    var areaOption = ReactAPI.international.getText("EAM.baseInfo.BaseInfo.installPlace");
    //设备类型
    var eamTypeOption = ReactAPI.international.getText("EAM.baseInfo.BaseInfo.eamType");

    $('div[name-key="EAM_1.0.0_baseInfo_eamViewLayout_baseInfo_nt"]').before("\\n            <div style=\\"text-align: center\\">" + labelText + ":  <select onchange=treeChangeView(this.value)  style=\\"border:solid 1px;margin-top:10px\\" >\\n                        <option value=\\"1\\">" + areaOption + "</option>\\n                        <option value=\\"2\\">" + eamTypeOption + "</option>\\n                       </select>\\n            </div>");
}, 1000);
"""

NEW_BODY_ES5 = """

function runWhenReactAPIReady(callback, retry) {
    retry = retry || 0;
    if (window.ReactAPI && ReactAPI.international && window.$) {
        callback();
        return;
    }
    if (retry < 50) {
        setTimeout(function () {
            runWhenReactAPIReady(callback, retry + 1);
        }, 200);
    }
}

runWhenReactAPIReady(function () {
    //类型
    var labelText = ReactAPI.international.getText("EAM.propertyshowName.randon1572243037103.flag");
    //区域位置
    var areaOption = ReactAPI.international.getText("EAM.baseInfo.BaseInfo.installPlace");
    //设备类型
    var eamTypeOption = ReactAPI.international.getText("EAM.baseInfo.BaseInfo.eamType");

    $('div[name-key="EAM_1.0.0_baseInfo_eamViewLayout_baseInfo_nt"]').before("\\n            <div style=\\"text-align: center\\">" + labelText + ":  <select onchange=treeChangeView(this.value)  style=\\"border:solid 1px;margin-top:10px\\" >\\n                        <option value=\\"1\\">" + areaOption + "</option>\\n                        <option value=\\"2\\">" + eamTypeOption + "</option>\\n                       </select>\\n            </div>");
});
"""

PATCHES = {
    TARGETS[0]: (OLD_BODY_JS, NEW_BODY_JS),
    TARGETS[1]: (OLD_BODY_ES5, NEW_BODY_ES5),
}


def static_root_default() -> Path:
    return PROJECT_ROOT / "runtime" / "bap-server" / "bap-workspace" / "bap-static"


def patch_file(path: Path, old: str, new: str) -> int:
    if not path.exists():
        raise FileNotFoundError(path)
    text = path.read_text(encoding="utf-8").replace("\r\n", "\n").replace("\n      \n", "\n\n")
    if new in text:
        return 0
    if old not in text:
        raise ValueError(f"expected EAM ReactAPI fragment not found: {path}")
    path.write_text(text.replace(old, new), encoding="utf-8")
    return 1


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Patch EAM low-code static pages to wait for ReactAPI before custom script execution."
    )
    parser.add_argument("--static-root", type=Path, default=static_root_default())
    args = parser.parse_args()

    changes = 0
    for relative, (old, new) in PATCHES.items():
        changes += patch_file(args.static_root / relative, old, new)
    state = "patched" if changes else "already patched"
    print(f"{state} EAM ReactAPI readiness static scripts: {args.static_root}")


if __name__ == "__main__":
    main()
