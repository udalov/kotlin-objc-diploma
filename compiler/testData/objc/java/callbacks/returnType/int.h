#import <Foundation/NSObject.h>

typedef int (*fun_t)();

@interface A : NSObject
+ (void) printOKIf42: (fun_t) fun;
@end
