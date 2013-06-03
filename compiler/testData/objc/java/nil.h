#import <Foundation/NSObject.h>

typedef struct _S S;

@interface A : NSObject
+ (id) getNilObject;
+ (A *) getNilA;
+ (Class) getNilClass;
+ (void *) getNullPointer;
+ (S *) getNullS;

+ (BOOL) isNilObject: (id) object;
+ (BOOL) isNilA: (A *) a;
+ (BOOL) isNilClass: (Class) class;
+ (BOOL) isNullPointer: (void *) pointer;
+ (BOOL) isNullS: (S *) s;
@end
