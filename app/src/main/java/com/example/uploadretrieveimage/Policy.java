package com.example.uploadretrieveimage;

import java.util.List;

public class Policy {
    private String policyId;
    private String ruleCombiningAlg;
    private String version;
    private String schemaLocation;
    private String desc;
    private List<Rule> rule;

    public String getPolicyId() {
        return policyId;
    }

    public void setPolicyId(String policyId) {
        this.policyId = policyId;
    }

    public String getRuleCombiningAlg() {
        return ruleCombiningAlg;
    }

    public void setRuleCombiningAlg(String ruleCombiningAlg) {
        this.ruleCombiningAlg = ruleCombiningAlg;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSchemaLocation() {
        return schemaLocation;
    }

    public void setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public List<Rule> getRule() {
        return rule;
    }

    public void setRule(List<Rule> rule) {
        this.rule = rule;
    }

    public static class Rule {
        private String ruleId;
        private String effect;
        private String desc;
        private List<Target> target;
        private List<Condition> condition;

        public String getRuleId() {
            return ruleId;
        }

        public void setRuleId(String ruleId) {
            this.ruleId = ruleId;
        }

        public String getEffect() {
            return effect;
        }

        public void setEffect(String effect) {
            this.effect = effect;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public List<Target> getTarget() {
            return target;
        }

        public void setTarget(List<Target> target) {
            this.target = target;
        }

        public List<Condition> getCondition() {
            return condition;
        }

        public void setCondition(List<Condition> condition) {
            this.condition = condition;
        }

        public static class Target {
            private List<AnyOf> anyOf;

            public List<AnyOf> getAnyOf() {
                return anyOf;
            }

            public void setAnyOf(List<AnyOf> anyOf) {
                this.anyOf = anyOf;
            }

            public static class AnyOf {
                private List<AllOf> allOf;

                public List<AllOf> getAllOf() {
                    return allOf;
                }

                public void setAllOf(List<AllOf> allOf) {
                    this.allOf = allOf;
                }

                public static class AllOf {
                    private Match match;

                    public Match getMatch() {
                        return match;
                    }

                    public void setMatch(Match match) {
                        this.match = match;
                    }

                    public static class Match {
                        private String matchId;
                        private AttributeValue attributeValue;
                        private AttributeDesignator attributeDesignator;

                        public String getMatchId() {
                            return matchId;
                        }

                        public void setMatchId(String matchId) {
                            this.matchId = matchId;
                        }

                        public AttributeValue getAttributeValue() {
                            return attributeValue;
                        }

                        public void setAttributeValue(AttributeValue attributeValue) {
                            this.attributeValue = attributeValue;
                        }

                        public AttributeDesignator getAttributeDesignator() {
                            return attributeDesignator;
                        }

                        public void setAttributeDesignator(AttributeDesignator attributeDesignator) {
                            this.attributeDesignator = attributeDesignator;
                        }

                        public static class AttributeValue {
                            private String value;
                            private String dataType;

                            public String getValue() {
                                return value;
                            }

                            public void setValue(String value) {
                                this.value = value;
                            }

                            public String getDataType() {
                                return dataType;
                            }

                            public void setDataType(String dataType) {
                                this.dataType = dataType;
                            }
                        }

                        public static class AttributeDesignator {
                            private boolean mustBePresent;
                            private String category;
                            private String attributeId;
                            private String dataType;

                            public boolean isMustBePresent() {
                                return mustBePresent;
                            }

                            public void setMustBePresent(boolean mustBePresent) {
                                this.mustBePresent = mustBePresent;
                            }

                            public String getCategory() {
                                return category;
                            }

                            public void setCategory(String category) {
                                this.category = category;
                            }

                            public String getAttributeId() {
                                return attributeId;
                            }

                            public void setAttributeId(String attributeId) {
                                this.attributeId = attributeId;
                            }

                            public String getDataType() {
                                return dataType;
                            }

                            public void setDataType(String dataType) {
                                this.dataType = dataType;
                            }
                        }
                    }
                }
            }
        }

        public static class Condition {
            private String functionId;
            private List<Apply> apply;

            public String getFunctionId() {
                return functionId;
            }

            public void setFunctionId(String functionId) {
                this.functionId = functionId;
            }

            public List<Apply> getApply() {
                return apply;
            }

            public void setApply(List<Apply> apply) {
                this.apply = apply;
            }

            public static class Apply {
                private String functionId;
                private AttributeValue attributeValue;
                private AttributeDesignator attributeDesignator;

                public String getFunctionId() {
                    return functionId;
                }

                public void setFunctionId(String functionId) {
                    this.functionId = functionId;
                }

                public AttributeValue getAttributeValue() {
                    return attributeValue;
                }

                public void setAttributeValue(AttributeValue attributeValue) {
                    this.attributeValue = attributeValue;
                }

                public AttributeDesignator getAttributeDesignator() {
                    return attributeDesignator;
                }

                public void setAttributeDesignator(AttributeDesignator attributeDesignator) {
                    this.attributeDesignator = attributeDesignator;
                }

                public static class AttributeValue {
                    private String value;
                    private String dataType;

                    public String getValue() {
                        return value;
                    }

                    public void setValue(String value) {
                        this.value = value;
                    }

                    public String getDataType() {
                        return dataType;
                    }

                    public void setDataType(String dataType) {
                        this.dataType = dataType;
                    }
                }

                public static class AttributeDesignator {
                    private boolean mustBePresent;
                    private String category;
                    private String attributeId;
                    private String dataType;

                    public boolean isMustBePresent() {
                        return mustBePresent;
                    }

                    public void setMustBePresent(boolean mustBePresent) {
                        this.mustBePresent = mustBePresent;
                    }

                    public String getCategory() {
                        return category;
                    }

                    public void setCategory(String category) {
                        this.category = category;
                    }

                    public String getAttributeId() {
                        return attributeId;
                    }

                    public void setAttributeId(String attributeId) {
                        this.attributeId = attributeId;
                    }

                    public String getDataType() {
                        return dataType;
                    }

                    public void setDataType(String dataType) {
                        this.dataType = dataType;
                    }
                }
            }
        }
    }
}
