"use strict";
var __rest = (this && this.__rest) || function (s, e) {
    var t = {};
    for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p) && e.indexOf(p) < 0)
        t[p] = s[p];
    if (s != null && typeof Object.getOwnPropertySymbols === "function")
        for (var i = 0, p = Object.getOwnPropertySymbols(s); i < p.length; i++) {
            if (e.indexOf(p[i]) < 0 && Object.prototype.propertyIsEnumerable.call(s, p[i]))
                t[p[i]] = s[p[i]];
        }
    return t;
};
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (Object.hasOwnProperty.call(mod, k)) result[k] = mod[k];
    result["default"] = mod;
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
var React = __importStar(require("react"));
var react_native_1 = require("react-native");
exports.PanoramaView = function (_a) {
    var onImageLoadingFailed = _a.onImageLoadingFailed, onImageLoaded = _a.onImageLoaded, dimensions = _a.dimensions, inputType = _a.inputType, props = __rest(_a, ["onImageLoadingFailed", "onImageLoaded", "dimensions", "inputType"]);
    var _onImageLoadingFailed = function () {
        if (onImageLoadingFailed) {
            onImageLoadingFailed();
        }
    };
    var _onImageLoaded = function () {
        if (onImageLoaded) {
            onImageLoaded();
        }
    };
    if (react_native_1.Platform.OS === "android" && !dimensions) {
        console.warn('The "dimensions" property is required for PanoramaView on Android devices.');
        return null;
    }
    if (react_native_1.Platform.OS === "ios" && inputType === "stereo") {
        console.warn("The stereo inputType is currently only supported on Android devices.");
    }
    return (<NativePanoramaView {...props} dimensions={dimensions} inputType={inputType} onImageLoaded={_onImageLoaded} onImageLoadingFailed={_onImageLoadingFailed}/>);
};
var NativePanoramaView = react_native_1.requireNativeComponent("PanoramaView");
