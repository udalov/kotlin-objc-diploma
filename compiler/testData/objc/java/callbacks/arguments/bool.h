#import <Foundation/NSObject.h>

typedef void (*fun_t)(BOOL, BOOL);

@interface A : NSObject
+ (void) invokeWithTrueAndFalse: (fun_t) fun;
@end
