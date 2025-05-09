package com.modforge.intellij.plugin.models;

import java.util.List;

/**
 * Represents data about a library.
 * Used for knowledge base enhancement.
 */
public class LibraryData {
    private String name;
    private String version;
    private String groupId;
    private String artifactId;
    private List<ClassData> classes;
    private long analyzedTimestamp;
    
    public LibraryData() {
        this.analyzedTimestamp = System.currentTimeMillis();
    }
    
    /**
     * Represents data about a class in a library.
     */
    public static class ClassData {
        private String name;
        private String packageName;
        private boolean isInterface;
        private boolean isEnum;
        private boolean isAbstract;
        private List<String> superClasses;
        private List<String> interfaces;
        private List<LibraryMethodData> methods;
        private List<FieldData> fields;
        private String javadoc;
        
        /**
         * Represents data about a field in a class.
         */
        public static class FieldData {
            private String name;
            private String type;
            private boolean isStatic;
            private boolean isFinal;
            private String javadoc;
            
            public String getName() {
                return name;
            }
            
            public void setName(String name) {
                this.name = name;
            }
            
            public String getType() {
                return type;
            }
            
            public void setType(String type) {
                this.type = type;
            }
            
            public boolean isStatic() {
                return isStatic;
            }
            
            public void setStatic(boolean isStatic) {
                this.isStatic = isStatic;
            }
            
            public boolean isFinal() {
                return isFinal;
            }
            
            public void setFinal(boolean isFinal) {
                this.isFinal = isFinal;
            }
            
            public String getJavadoc() {
                return javadoc;
            }
            
            public void setJavadoc(String javadoc) {
                this.javadoc = javadoc;
            }
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
            
            // Extract package name from full class name
            int lastDot = name.lastIndexOf('.');
            if (lastDot > 0) {
                this.packageName = name.substring(0, lastDot);
            } else {
                this.packageName = "";
            }
        }
        
        public String getPackageName() {
            return packageName;
        }
        
        public boolean isInterface() {
            return isInterface;
        }
        
        public void setInterface(boolean isInterface) {
            this.isInterface = isInterface;
        }
        
        public boolean isEnum() {
            return isEnum;
        }
        
        public void setEnum(boolean isEnum) {
            this.isEnum = isEnum;
        }
        
        public boolean isAbstract() {
            return isAbstract;
        }
        
        public void setAbstract(boolean isAbstract) {
            this.isAbstract = isAbstract;
        }
        
        public List<String> getSuperClasses() {
            return superClasses;
        }
        
        public void setSuperClasses(List<String> superClasses) {
            this.superClasses = superClasses;
        }
        
        public List<String> getInterfaces() {
            return interfaces;
        }
        
        public void setInterfaces(List<String> interfaces) {
            this.interfaces = interfaces;
        }
        
        public List<LibraryMethodData> getMethods() {
            return methods;
        }
        
        public void setMethods(List<LibraryMethodData> methods) {
            this.methods = methods;
        }
        
        public List<FieldData> getFields() {
            return fields;
        }
        
        public void setFields(List<FieldData> fields) {
            this.fields = fields;
        }
        
        public String getJavadoc() {
            return javadoc;
        }
        
        public void setJavadoc(String javadoc) {
            this.javadoc = javadoc;
        }
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        
        // Try to extract groupId and artifactId from name
        if (name.contains(":")) {
            String[] parts = name.split(":");
            if (parts.length >= 2) {
                this.groupId = parts[0];
                this.artifactId = parts[1];
                
                // Extract version if available
                if (parts.length >= 3) {
                    this.version = parts[2];
                }
            }
        }
    }
    
    public String getVersion() {
        return version;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    
    public String getArtifactId() {
        return artifactId;
    }
    
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }
    
    public List<ClassData> getClasses() {
        return classes;
    }
    
    public void setClasses(List<ClassData> classes) {
        this.classes = classes;
    }
    
    public long getAnalyzedTimestamp() {
        return analyzedTimestamp;
    }
    
    public void setAnalyzedTimestamp(long analyzedTimestamp) {
        this.analyzedTimestamp = analyzedTimestamp;
    }
}