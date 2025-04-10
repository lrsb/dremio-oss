/*
 * Copyright (C) 2017-2019 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { shallow, mount } from "enzyme";
import { stubArrayFieldMethods } from "testUtil";

import TextFieldList from "./TextFieldList";

describe("TextFieldList", () => {
  let minimalProps;
  let commonProps;
  beforeEach(() => {
    minimalProps = {};
    commonProps = {
      ...minimalProps,
      arrayField: stubArrayFieldMethods([]),
    };
  });

  it("should render with minimal props without exploding", () => {
    const wrapper = shallow(<TextFieldList {...minimalProps} />);
    expect(wrapper).to.have.length(1);
  });

  it("should render all passed fields from arrayField", () => {
    const arrayField = [
      { value: "1", readOnly: true },
      { value: "2", readOnly: true },
    ];
    const wrapper = mount(
      <TextFieldList {...minimalProps} arrayField={arrayField} />,
    );
    expect(wrapper.find("TextField")).to.have.length(2);
  });

  it("should render text fields which values found with fieldKey when fieldKey passed to props", () => {
    const arrayField = [
      { some: { nested: { value: "fieldValue", readOnly: true } } },
    ];
    const wrapper = mount(
      <TextFieldList
        {...minimalProps}
        arrayField={arrayField}
        fieldKey={"some.nested"}
      />,
    );
    expect(wrapper.find("TextField").props()).to.have.property(
      "value",
      "fieldValue",
    );
  });

  describe("#addItem", () => {
    let wrapper;
    let instance;
    beforeEach(() => {
      wrapper = shallow(<TextFieldList {...commonProps} />);
      instance = wrapper.instance();
    });

    it("should add new item with empty string when newItemDefaultValue not passed to props", () => {
      instance.addItem();
      expect(commonProps.arrayField.addField).to.be.calledWith("");
    });

    it("should add new item according to newItemDefaultValue from props", () => {
      wrapper.setProps({ newItemDefaultValue: { key: "key", value: "value" } });
      instance.addItem();
      expect(commonProps.arrayField.addField).to.be.calledWith({
        key: "key",
        value: "value",
      });
    });
  });
});
