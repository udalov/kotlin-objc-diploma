#import "nil.h"

@implementation A
+ (id) getNilObject {
    return nil;
}
+ (A *) getNilA {
    return nil;
}
+ (Class) getNilClass {
    return Nil;
}
+ (void *) getNullPointer {
    return NULL;
}
+ (S *) getNullS {
    return NULL;
}


+ (BOOL) isNilObject: (id) object {
    return object == nil;
}
+ (BOOL) isNilA: (A *) a {
    return a == nil;
}
+ (BOOL) isNilClass: (Class) class {
    return class == Nil;
}
+ (BOOL) isNullPointer: (void *) pointer {
    return pointer == NULL;
}
+ (BOOL) isNullS: (S *) s {
    return s == NULL;
}
@end
