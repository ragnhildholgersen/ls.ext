(function (root, factory) {
  if (typeof define === 'function' && define.amd) {
    // AMD. Register as an anonymous module.
    define([], factory);
  } else if (typeof module === 'object' && module.exports) {
    // Node. Does not work with strict CommonJS, but
    // only CommonJS-like environments that support module.exports,
    // like Node.
    module.exports = factory();
  } else {
    // Browser globals (root is window)
    root.returnExports = factory();
  }
}(this, function () {

  // propsByClass extract the properties from the ontology which are valid for a given class.
  // This includes properties with unspecified domain (applicable for all classes), and those
  // with specified range as the given class.
  function propsByClass(ontology, cls) {
    var res = [];
    ontology["@graph"].forEach(function (p) {
      if (p["@type"] === "rdfs:Property") {
        if (p["rdfs:domain"] === undefined || p["rdfs:domain"]["@id"] === "rdfs:Class") {
          res.push(p);
        } else if (Array.isArray(p["rdfs:domain"])) {
          p["rdfs:domain"].forEach(function (q) {
            if (q["@id"] === ("deichman:" + cls)) {
              res.push(p);
            }
          });
        } else if (p["rdfs:domain"]["@id"] === ("deichman:" + cls)) {
          res.push(p);
        }
      }
    });
    return res;
  }

  // resolveURI resolves a URI in prefixed form to its full form, according to prefixes specified
  // in the ontology.
  function resolveURI(ontology, uri) {
    var i = uri.indexOf(":");
    var prefix = uri.substr(0, i);
    for (var k in ontology["@context"]) {
      if (prefix === k) {
        return ontology["@context"][k] + uri.substr(i + 1);
      }
    }
  }

  // validateLiteral checks that a given value conforms to it's xsd:range(datatype).
  function validateLiteral(value, range) {
    switch (range) {
      case "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString":
      case "http://www.w3.org/2001/XMLSchema#string":
        return true; // a javscript string is always valid as xsd:string
      case "http://www.w3.org/2001/XMLSchema#gYear":
        // According to its specification, a xsd:gYear allows time-zone information, but
        // we don't want that, and only accepts negative (BCE) or positive (CE) integers.
        // TODO shall we require 0-padding - i.e not allow "92" but require "0092"?
        return /^-?(\d){1,4}$/.test(value);
      case "http://www.w3.org/2001/XMLSchema#nonNegativeInteger":
        return /^\+?(\d)+$/.test(value);
      default:
        var err = "don't know how to validate literal of range: <" + range + ">";
        throw err;
    }
  }

  // createPatch creates a patch request for a given subject, predicate and value (el)
  // as it is represented in the client UI.
  function createPatch(subject, predicate, el) {
    var addPatch,
        delPatch;

    if (el.current.value !== "") {
      addPatch = { op: "add", s: subject, p: predicate, o: { value: el.current.value } };
      if (el.current.lang !== "") {
        addPatch.o.lang = el.current.lang;
      }

      if (el.current.datatype !== "") {
        addPatch.o.datatype = el.current.datatype;
      }
    }

    if (el.old.value !== "") {
      delPatch = { op: "del", s: subject, p: predicate, o: { value: el.old.value } };
      if (el.old.lang !== "") {
        delPatch.o.lang = el.old.lang;
      }

      if (el.old.datatype !== "") {
        delPatch.o.datatype = el.old.datatype;
      }
    }

    if (delPatch && addPatch) {
      return JSON.stringify([delPatch, addPatch]);
    } else if (delPatch) {
      return JSON.stringify(delPatch);
    } else {
      return JSON.stringify(addPatch);
    }
  }

  // extractValues extracts the values (properties) from the given resource in JSON-LD format,
  // and returns them in the format which the client UI uses.
  // TODO evaluate JSON-LD messiness and ambiguity VS parsing N-Triples.
  function extractValues(resource) {
    var values = {};
    for (var prop in resource) {
      switch (prop) {
        case "@id":
        case "@type":
        case "@context":
          break;
        default:
          var predicate = resolveURI(resource,  prop);
          if (typeof resource[prop] === "string") {

            // property has one value, a simple string literal
            values[predicate] = [{ old: { value: resource[prop], type: "", lang: "" },
                                  current: { value: resource[prop], type: "", lang: "" } }];
          } else if (Array.isArray(resource[prop])) {

            // property has several values
            values[predicate] = [];
            for (var v in resource[prop]) {
              var val = resource[prop][v];
              if (typeof val === "string") {
                values[predicate].push({ old: { value: val, type: "", lang: "" },
                                       current: { value: val, type: "", lang: "" } });
              } else if (typeof val === "object") {
                values[predicate].push({ old: { value: val["@value"], type: "", lang: val["@language"] },
                                       current: { value: val["@value"], type: "", lang: val["@language"] } });
              }
            }
          } else if (typeof resource[prop] === "object") {

            // property has one value, with language tag or datatype
            values[predicate] = [{ old: { value: resource[prop]["@value"], type: "", lang: resource[prop]["@language"] },
                                  current: { value: resource[prop]["@value"], type: "", lang: resource[prop]["@language"] } }];
          }
      }
    }

    return values;
  }

  function resourceLabel(ontology, cls, lang) {
    return ontology["@graph"].filter(
      function (e) {
        return e["@id"] == "deichman:" + cls;
      }
           )[0]["rdfs:label"].filter(
        function (e) {
          return e["@language"] == lang;
        })[0]["@value"];
  }

  // exported functions
  return {
    propsByClass: propsByClass,
    createPatch: createPatch,
    resolveURI: resolveURI,
    validateLiteral: validateLiteral,
    extractValues: extractValues,
    resourceLabel: resourceLabel
  };
}));